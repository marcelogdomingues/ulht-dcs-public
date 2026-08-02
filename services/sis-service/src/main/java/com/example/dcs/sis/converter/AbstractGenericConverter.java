package com.example.dcs.sis.converter;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public abstract class AbstractGenericConverter<S, T> implements GenericConverter<S, T> {

    @Override
    @Nullable
    public T convert(@Nullable S source) {
        if (source == null) {
            return null;
        }
        return doConvert(source);
    }

    @Override
    @Nullable
    public T convert(@Nullable S source, @NonNull Class<? extends T> targetClass) {
        if (source == null) {
            return null;
        }
        return doConvert(source);
    }

    protected abstract T doConvert(@NonNull S source);
}