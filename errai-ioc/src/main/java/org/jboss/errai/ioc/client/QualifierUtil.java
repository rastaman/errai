package org.jboss.errai.ioc.client;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for testing the equality of qualifiers at runtime.
 *
 * @author Mike Brock
 */
public class QualifierUtil {
  public static final Annotation DEFAULT_ANNOTATION = new Default() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Default.class;
    }
  };

  public static final Annotation ANY_ANNOTATION = new Any() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return Any.class;
    }
  };

  public static final Annotation[] DEFAULT_QUALIFIERS = new Annotation[]{
      DEFAULT_ANNOTATION,
      ANY_ANNOTATION
  };

  private static final Map<String, Annotation> DEFAULT_MATCHING_MAP = new HashMap<String, Annotation>() {
    {
      for (final Annotation a : DEFAULT_QUALIFIERS) {
        put(a.annotationType().getName(), a);
      }
    }
  };

  private static QualifierEqualityFactoryProvider factoryProvider;
  private static QualifierEqualityFactory factory;

  public static void init() {
    if (factory == null)
      factory = factoryProvider.provide();
  }

  public static boolean isEqual(final Annotation a1, final Annotation a2) {
    return factory.isEqual(a1, a2);
  }

  public static boolean isSameType(final Annotation a1, final Annotation a2) {
    return !(a1 == null || a2 == null) && a1.annotationType().equals(a2.annotationType());
  }

  public static int hashCodeOf(final Annotation a1) {

    return factory.hashCodeOf(a1);
  }

  public static boolean matches(final Collection<Annotation> allOf, final Collection<Annotation> in) {
    if (allOf.isEmpty()) return true;

    final Map<String, Annotation> allOfMap = new HashMap<String, Annotation>();
    final Map<String, Annotation> inMap = new HashMap<String, Annotation>();

    for (final Annotation a : in) {
      inMap.put(a.annotationType().getName(), a);
    }

    for (final Annotation a : allOf) {
      allOfMap.put(a.annotationType().getName(), a);
    }

    if (!inMap.keySet().containsAll(allOfMap.keySet())) {
      return false;
    }

    if (factory != null) {
      for (final Map.Entry<String, Annotation> entry : allOfMap.entrySet()) {
        if (!factory.isEqual(entry.getValue(), inMap.get(entry.getKey()))) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean isDefaultAnnotations(final Annotation[] annotations) {
    return annotations == null || isDefaultAnnotations(Arrays.asList(annotations));
  }


  public static boolean isDefaultAnnotations(final Collection<Annotation> annotations) {
    return annotations == null || (annotations.size() == 2 && matches(DEFAULT_MATCHING_MAP.values(), annotations));
  }

  public static void initFromFactoryProvider(QualifierEqualityFactoryProvider provider) {
    factoryProvider = provider;
    factory = null;
    init();
  }

  public static int hashValueFor(final int i) {
    return i;
  }

  public static int hashValueFor(final boolean b) {
    return b ? 1 : 0;
  }

  public static int hashValueFor(final long l) {
    return (int) l;
  }

  public static int hashValueFor(final float f) {
    return (int) f * 1000;
  }

  public static int hashValueFor(final double d) {
    return (int) d * 1000;
  }

  public static int hashValueFor(final char c) {
    return (int) c;
  }

  public static int hashValueFor(final byte b) {
    return (int) b;
  }

  public static int hashValueFor(final short s) {
    return (int) s;
  }

  public static int hashValueFor(final Object o) {
    if (o == null) return -1;
    return o.hashCode();
  }

  public static Annotation[] getDefaultQualifiers() {
    return DEFAULT_QUALIFIERS;
  }
}
