package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

public enum OrderStatus {
    ORDER, CANCEL
}
