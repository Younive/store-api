package com.younive.store.products;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Byte category_id;
}
