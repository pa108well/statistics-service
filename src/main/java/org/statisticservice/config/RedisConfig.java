package org.statisticservice.config;


import org.statisticservice.repository.dto.CountryStatisticsDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, CountryStatisticsDto> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<CountryStatisticsDto> valueSerializer = new Jackson2JsonRedisSerializer<>(CountryStatisticsDto.class);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializationContext<String, CountryStatisticsDto> context = RedisSerializationContext
                .<String, CountryStatisticsDto>newSerializationContext(stringSerializer)
                .value(valueSerializer)
                .hashKey(stringSerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
