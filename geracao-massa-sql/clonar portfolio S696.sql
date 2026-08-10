-- Parâmetros de controle
DECLARE @SQ_PTF_ACO_ORIGEM INT = 56;  -- ID de onde os dados serão copiados
DECLARE @SQ_PTF_ACO_DESTINO INT = 99; -- ID do seu novo portfólio (já existente)

-- Variáveis de navegação
DECLARE @OldAcoID INT, @NewAcoID INT;
DECLARE @OldEtpID INT, @NewEtpID INT;

-- 1. Início do loop de Ações (T696ACES)
DECLARE C_Acoes CURSOR FOR 
SELECT SQ_ACO_ETT FROM [dbo].[T696ACES] WHERE SQ_PTF_ACO = @SQ_PTF_ACO_ORIGEM;

OPEN C_Acoes;
FETCH NEXT FROM C_Acoes INTO @OldAcoID;

WHILE @@FETCH_STATUS = 0
BEGIN
    -- Insere a Ação com ST_ACO_ETT fixo em 10 e limpa histórico
    INSERT INTO [dbo].[T696ACES] 
        ([NM_ACO_ETT], [DE_ACO_ETT], [SQ_ORI_ACO], [CD_UND], [DH_LIM], [MT_USU_ALT], [NM_USU_ALT], [DH_ALT], [ST_ACO_ETT], [DE_HST], [MT_USU_RSP], [CD_UND_APR], [SQ_PTF_ACO], [TP_ACO_ETT])
    SELECT 
        [NM_ACO_ETT], [DE_ACO_ETT], [SQ_ORI_ACO], [CD_UND], [DH_LIM], [MT_USU_ALT], [NM_USU_ALT], GETDATE(), 
        10,   -- Valor Fixo solicitado
        NULL, -- Limpa histórico da ação
        [MT_USU_RSP], [CD_UND_APR], @SQ_PTF_ACO_DESTINO, [TP_ACO_ETT]
    FROM [dbo].[T696ACES] 
    WHERE [SQ_ACO_ETT] = @OldAcoID;

    SET @NewAcoID = SCOPE_IDENTITY();

    -- 2. Clonar Vínculos e Domínios
    INSERT INTO [dbo].[T696VIAC] (SQ_ACO_ETT, SQ_ORT_ETT)
    SELECT @NewAcoID, SQ_ORT_ETT FROM [dbo].[T696VIAC] WHERE SQ_ACO_ETT = @OldAcoID;

    INSERT INTO [dbo].[T696DOAC] (SQ_ACO_ETT, SQ_DMN)
    SELECT @NewAcoID, SQ_DMN FROM [dbo].[T696DOAC] WHERE SQ_ACO_ETT = @OldAcoID;

    -- 3. Início do loop de Etapas (T696ETAC)
    DECLARE C_Etapas CURSOR FOR SELECT SQ_ETP_ACO FROM [dbo].[T696ETAC] WHERE SQ_ACO_ETT = @OldAcoID;
    
    OPEN C_Etapas;
    FETCH NEXT FROM C_Etapas INTO @OldEtpID;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        -- Insere a Etapa com os valores fixos: status 1, progresso 0, rep 0
        INSERT INTO [dbo].[T696ETAC] 
            ([SQ_ACO_ETT], [CD_UND], [DE_ETP_ACO], [DE_ENT_ETP_ACO], [DH_ALT], [MT_USU_ALT], [NM_USU_ALT], [ST_ETP_ACO], [AA_REA], [PC_REA_TOT], [ST_REP])
        SELECT 
            @NewAcoID, [CD_UND], [DE_ETP_ACO], [DE_ENT_ETP_ACO], GETDATE(), [MT_USU_ALT], [NM_USU_ALT], 
            1,   -- ST_ETP_ACO fixo
            [AA_REA], 
            0,   -- PC_REA_TOT fixo
            0    -- ST_REP fixo
        FROM [dbo].[T696ETAC] 
        WHERE [SQ_ETP_ACO] = @OldEtpID;

        SET @NewEtpID = SCOPE_IDENTITY();

        -- 4. Clonar Entregas (T696ENAC) - MANTENDO PC_PLJ_ENT original
        INSERT INTO [dbo].[T696ENAC] 
            ([SQ_ETP_ACO], [MM_ENT], [PC_PLJ_ENT], [PC_REA_ENT], [DE_REA_ENT], [DH_ALT], [MT_USU_ALT], [NM_USU_ALT], [ST_ENT_ACO], [DE_HST], [NT_REV])
        SELECT 
            @NewEtpID, 
            [MM_ENT], 
            [PC_PLJ_ENT], -- MANTIDO o valor original conforme solicitado
            NULL,         -- PC_REA_ENT limpo
            NULL,         -- DE_REA_ENT limpo
            GETDATE(), [MT_USU_ALT], [NM_USU_ALT], [ST_ENT_ACO], 
            NULL,         -- DE_HST limpo
            NULL          -- NT_REV limpo
        FROM [dbo].[T696ENAC] 
        WHERE [SQ_ETP_ACO] = @OldEtpID;

        FETCH NEXT FROM C_Etapas INTO @OldEtpID;
    END;
    CLOSE C_Etapas;
    DEALLOCATE C_Etapas;

    FETCH NEXT FROM C_Acoes INTO @OldAcoID;
END;

CLOSE C_Acoes;
DEALLOCATE C_Acoes;

PRINT 'Carga de massa de dados realizada. Planejamento (PC_PLJ_ENT) preservado.';