package run.halo.app.extension.index.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.relational.core.sql.Visitable;
import org.springframework.data.relational.core.sql.Visitor;
import run.halo.app.extension.Extension;
import run.halo.app.extension.index.Indices;
import run.halo.app.extension.index.LabelIndexQuery;
import run.halo.app.extension.index.ValueIndexQuery;

/**
 * A visitor that visits a query and returns the matching extension names.
 *
 * @param <E> the type of extension
 * @author johnniang
 * @since 2.22.0
 */
public class QueryVisitor<E extends Extension> implements Visitor {

    private final ConversionService conversionService;

    private final Indices<E> indices;

    private final Set<String> result;

    public QueryVisitor(Indices<E> indices, ConversionService conversionService) {
        this.indices = indices;
        this.result = new HashSet<>();
        this.conversionService = conversionService;
    }

    @Override
    public void enter(Visitable segment) {
        var visitor = new ConditionVisitor();
        segment.visit(visitor);
        result.addAll(visitor.getResult());
    }

    public Set<String> getResult() {
        return result;
    }

    class ConditionVisitor implements Visitor {

        @Getter
        private final Set<String> result;

        ConditionVisitor() {
            result = new HashSet<>();
        }

        @Override
        public void enter(Visitable segment) {
            if (segment instanceof And and) {
                var left = and.left();
                var right = and.right();
                // delegate to AndCondition for backward compatibility
                new AndCondition(left, right).visit(this);
            } else if (segment instanceof EmptyCondition) {
                result.addAll(allQuery("metadata.name", false));
            } else if (segment instanceof AndCondition andCondition) {
                var left = andCondition.left();
                var right = andCondition.right();
                if (left instanceof EmptyCondition) {
                    right.visit(this);
                    return;
                }
                if (right instanceof EmptyCondition) {
                    left.visit(this);
                    return;
                }
                left.visit(this);
                // fail fast if left result is empty
                if (!result.isEmpty()) {
                    var rightVisitor = new ConditionVisitor();
                    right.visit(rightVisitor);
                    result.retainAll(rightVisitor.getResult());
                }
            } else if (segment instanceof OrCondition orCondition) {
                var left = orCondition.left();
                var right = orCondition.right();
                if (left instanceof EmptyCondition) {
                    left.visit(this);
                    return;
                }
                if (right instanceof EmptyCondition) {
                    right.visit(this);
                    return;
                }
                left.visit(this);
                var rightVisitor = new ConditionVisitor();
                right.visit(rightVisitor);
                result.addAll(rightVisitor.getResult());
            } else if (segment instanceof NotCondition notCondition) {
                var condition = notCondition.condition();
                if (condition instanceof EmptyCondition) {
                    return;
                }
                condition.not().visit(this);
            } else if (segment instanceof EqualCondition condition) {
                result.addAll(equalQuery(condition.indexName(), condition.key(), false));
            } else if (segment instanceof NotEqualCondition condition) {
                result.addAll(equalQuery(condition.indexName(), condition.key(), true));
            } else if (segment instanceof InCondition condition) {
                result.addAll(inQuery(condition.indexName(), condition.keys(), false));
            } else if (segment instanceof NotInCondition condition) {
                result.addAll(inQuery(condition.indexName(), condition.keys(), true));
            } else if (segment instanceof LessThanCondition condition) {
                result.addAll(lessThanQuery(condition.indexName(), condition.upperBound(), condition.inclusive(),
                    false));
            } else if (segment instanceof GreaterThanCondition condition) {
                result.addAll(lessThanQuery(condition.indexName(), condition.lowerBound(), condition.inclusive(),
                    true));
            } else if (segment instanceof BetweenCondition bc) {
                result.addAll(betweenQuery(bc.indexName(), bc.fromKey(), bc.fromInclusive(), bc.toKey(),
                    bc.toInclusive(), false));
            } else if (segment instanceof NotBetweenCondition nbc) {
                result.addAll(betweenQuery(nbc.indexName(), nbc.fromKey(), nbc.fromInclusive(), nbc.toKey(),
                    nbc.toInclusive(), true));
            } else if (segment instanceof IsNullCondition condition) {
                result.addAll(isNullQuery(condition.indexName(), false));
            } else if (segment instanceof IsNotNullCondition condition) {
                result.addAll(isNullQuery(condition.indexName(), true));
            } else if (segment instanceof StringContainsCondition condition) {
                result.addAll(stringContainsQuery(condition.indexName(), condition.keyword(), false));
            } else if (segment instanceof StringNotContainsCondition condition) {
                result.addAll(stringContainsQuery(condition.indexName(), condition.keyword(), true));
            } else if (segment instanceof StringStartsWithCondition condition) {
                result.addAll(stringStartsWithQuery(condition.indexName(), condition.prefix(), false));
            } else if (segment instanceof StringNotStartsWithCondition condition) {
                result.addAll(stringStartsWithQuery(condition.indexName(), condition.prefix(), true));
            } else if (segment instanceof StringEndsWithCondition condition) {
                result.addAll(stringEndsWithQuery(condition.indexName(), condition.suffix(), false));
            } else if (segment instanceof StringNotEndsWithCondition condition) {
                result.addAll(stringEndsWithQuery(condition.indexName(), condition.suffix(), true));
            } else if (segment instanceof AllCondition condition) {
                result.addAll(allQuery(condition.indexName(), false));
            } else if (segment instanceof NoneCondition condition) {
                result.addAll(allQuery(condition.indexName(), true));
            } else if (segment instanceof LabelExistsCondition condition) {
                result.addAll(labelExistsQuery(condition.labelKey()));
            } else if (segment instanceof LabelNotExistsCondition condition) {
                // To get all extensions that do not have the label, we get all extensions
                result.addAll(allQuery("metadata.name", false));
                result.removeAll(labelExistsQuery(condition.labelKey()));
            } else if (segment instanceof LabelEqualsCondition condition) {
                result.addAll(labelEqualsQuery(condition.labelKey(), condition.labelValue(), false));
            } else if (segment instanceof LabelNotEqualsCondition condition) {
                // Only for backward compatibility
                result.addAll(allQuery("metadata.name", false));
                result.removeAll(labelEqualsQuery(condition.labelKey(), condition.labelValue(), false));
            } else if (segment instanceof LabelInCondition condition) {
                result.addAll(labelInQuery(condition.labelKey(), condition.labelValues(), false));
            } else if (segment instanceof LabelNotInCondition condition) {
                result.addAll(labelInQuery(condition.labelKey(), condition.labelValues(), true));
            }
        }

        private Set<String> labelInQuery(String labelKey, Collection<String> labelValues, boolean negated) {
            var index = this.getLabelIndexQuery();
            if (negated) {
                return index.notIn(labelKey, labelValues);
            }
            return index.in(labelKey, labelValues);
        }

        private Set<String> labelEqualsQuery(String labelKey, String labelValue, boolean negated) {
            var index = this.getLabelIndexQuery();
            if (negated) {
                return index.notEqual(labelKey, labelValue);
            }
            return index.equal(labelKey, labelValue);
        }

        private Set<String> labelExistsQuery(String labelKey) {
            var index = getLabelIndexQuery();
            return index.exists(labelKey);
        }

        private Set<String> stringEndsWithQuery(String indexName, String suffix, boolean negated) {
            var index = getValueIndexQuery(indexName);
            if (negated) {
                return index.stringNotEndsWith(suffix);
            }
            return index.stringEndsWith(suffix);
        }

        private Set<String> allQuery(String indexName, boolean negated) {
            var index = getValueIndexQuery(indexName);
            if (negated) {
                return Set.of();
            }
            return index.all();
        }

        private Set<String> stringStartsWithQuery(String indexName, String prefix, boolean negated) {
            var index = getValueIndexQuery(indexName);
            if (negated) {
                return index.stringNotStartsWith(prefix);
            }
            return index.stringStartsWith(prefix);
        }

        private Set<String> stringContainsQuery(String indexName, String keyword, boolean negated) {
            var index = getValueIndexQuery(indexName);
            if (negated) {
                return index.stringNotContains(keyword);
            }
            return index.stringContains(keyword);
        }

        private <K extends Comparable<K>> Set<String> isNullQuery(String indexName, boolean negated) {
            var index = this.<K>getValueIndexQuery(indexName);
            if (negated) {
                return index.isNotNull();
            }
            return index.isNull();
        }

        private <K extends Comparable<K>> Set<String> betweenQuery(
                String indexName,
                Object fromKey,
                boolean fromInclusive,
                Object toKey,
                boolean toInclusive,
                boolean negated) {
            var index = this.<K>getValueIndexQuery(indexName);
            if (!conversionService.canConvert(fromKey.getClass(), index.getKeyType())) {
                throw new IllegalArgumentException(
                        "Cannot convert key: " + fromKey + " to type: " + index.getKeyType());
            }
            if (!conversionService.canConvert(toKey.getClass(), index.getKeyType())) {
                throw new IllegalArgumentException("Cannot convert key: " + toKey + " to type: " + index.getKeyType());
            }
            if (negated) {
                return index.notBetween(
                        conversionService.convert(fromKey, index.getKeyType()),
                        fromInclusive,
                        conversionService.convert(toKey, index.getKeyType()),
                        toInclusive);
            } else {
                return index.between(
                        conversionService.convert(fromKey, index.getKeyType()),
                        fromInclusive,
                        conversionService.convert(toKey, index.getKeyType()),
                        toInclusive);
            }
        }

        private <K extends Comparable<K>> Set<String> lessThanQuery(
                String indexName, Object bound, boolean inclusive, boolean negated) {
            var index = this.<K>getValueIndexQuery(indexName);
            if (!conversionService.canConvert(bound.getClass(), index.getKeyType())) {
                throw new IllegalArgumentException("Cannot convert key: " + bound + " to type: " + index.getKeyType());
            }
            if (negated) {
                return index.greaterThan(conversionService.convert(bound, index.getKeyType()), inclusive);
            } else {
                return index.lessThan(conversionService.convert(bound, index.getKeyType()), inclusive);
            }
        }

        private <K extends Comparable<K>> Set<String> equalQuery(String indexName, Object key, boolean negated) {
            var index = this.<K>getValueIndexQuery(indexName);
            if (!conversionService.canConvert(key.getClass(), index.getKeyType())) {
                throw new IllegalArgumentException("Cannot convert key: " + key + " to type: " + index.getKeyType());
            }
            if (negated) {
                return index.notEqual(conversionService.convert(key, index.getKeyType()));
            } else {
                return index.equal(conversionService.convert(key, index.getKeyType()));
            }
        }

        private <K extends Comparable<K>> Set<String> inQuery(
                String indexName, Collection<Object> keys, boolean negated) {
            var index = this.<K>getValueIndexQuery(indexName);
            var convertedKeys = keys.stream()
                    .map(key -> {
                        if (!conversionService.canConvert(key.getClass(), index.getKeyType())) {
                            throw new IllegalArgumentException(
                                    "Cannot convert key: " + key + " to type: " + index.getKeyType());
                        }
                        return conversionService.convert(key, index.getKeyType());
                    })
                    .toList();
            if (negated) {
                return index.notIn(convertedKeys);
            } else {
                return index.in(convertedKeys);
            }
        }

        private <K extends Comparable<K>> ValueIndexQuery<K> getValueIndexQuery(String indexName) {
            var index = indices.<K>getIndex(indexName);
            if (!(index instanceof ValueIndexQuery<?> valueIndexQuery)) {
                throw new IllegalArgumentException("Index is not in-memory: " + indexName);
            }
            return (ValueIndexQuery<K>) valueIndexQuery;
        }

        private LabelIndexQuery getLabelIndexQuery() {
            var indexName = "metadata.labels";
            var index = indices.<String>getIndex(indexName);
            if (!(index instanceof LabelIndexQuery labelIndexQuery)) {
                throw new IllegalArgumentException("Index is not a label index: " + indexName);
            }
            return labelIndexQuery;
        }
    }
}
