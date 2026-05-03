package com.younive.store.products;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(source = "category.id", target="category_id")
    ProductDto toDto(Product product);
    Product toEntity(ProductDto request);
    @Mapping(target = "id", ignore = true)
    void updateEntity(ProductDto request, @MappingTarget Product product);
}
