package com.example.dcs.sis.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

public interface GenericConverter<S, T> extends Converter<S, T> {

    T convert(S source, @NonNull Class<? extends T> targetClass);
}