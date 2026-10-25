package com.manager.money.exchange.manager.Strategy;

import com.manager.money.exchange.manager.User.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class ValidationService {

    private final Map<Class<?>, IValidationStrategy<?>> strategies;

    public ValidationService(List<IValidationStrategy<?>> list) {
        Map<Class<?>, IValidationStrategy<?>> map = new HashMap<>();
        for (IValidationStrategy<?> s : list) {
            if (map.put(s.supports(), s) != null)
                throw new IllegalStateException(
                        "Strategy duplicada para " + s.supports().getSimpleName());
        }
        this.strategies = Map.copyOf(map);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ValidationError> validate(T dto, User user) {
        Objects.requireNonNull(dto, "dto nulo");
        Objects.requireNonNull(user, "user nulo");
        IValidationStrategy<T> s = (IValidationStrategy<T>) strategies.get(dto.getClass());
        if (s == null)
            throw new IllegalStateException(
                    "Sem validação para " + dto.getClass().getSimpleName());
        return s.validate(dto, user);
    }
}