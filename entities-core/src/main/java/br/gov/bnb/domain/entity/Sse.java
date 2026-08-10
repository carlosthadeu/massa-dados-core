package br.gov.bnb.domain.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name="T696SSE")
public class Sse implements Serializable {

    @Column(name = "SQ_SSE", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sqSse;

    public Long getSqSse() {
        return sqSse;
    }

    public void setSqSse(Long sqSse) {
        this.sqSse = sqSse;
    }

}
