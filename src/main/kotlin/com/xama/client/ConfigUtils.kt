package com.xama.client

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.zalando.riptide.HttpBuilder
import org.zalando.riptide.OriginalStackTracePlugin

fun configureObjectMapper(builder: Jackson2ObjectMapperBuilder) = builder
        .modulesToInstall(KotlinModule())
        .createXmlMapper(false)
        .propertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
        .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)


fun configureHttp(builder: HttpBuilder, objectMapper: ObjectMapper) = builder
        .converter(MappingJackson2HttpMessageConverter(objectMapper))
        .converter(FormHttpMessageConverter())
        .plugin(OriginalStackTracePlugin())
