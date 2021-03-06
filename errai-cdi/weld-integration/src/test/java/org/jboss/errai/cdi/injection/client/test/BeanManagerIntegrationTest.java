package org.jboss.errai.cdi.injection.client.test;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;

import org.jboss.errai.cdi.injection.client.*;
import org.jboss.errai.cdi.injection.client.qualifier.LincolnBar;
import org.jboss.errai.cdi.injection.client.qualifier.QualA;
import org.jboss.errai.cdi.injection.client.qualifier.QualAppScopeBeanA;
import org.jboss.errai.cdi.injection.client.qualifier.QualAppScopeBeanB;
import org.jboss.errai.cdi.injection.client.qualifier.QualB;
import org.jboss.errai.cdi.injection.client.qualifier.QualEnum;
import org.jboss.errai.cdi.injection.client.qualifier.QualParmAppScopeBeanApples;
import org.jboss.errai.cdi.injection.client.qualifier.QualParmAppScopeBeanOranges;
import org.jboss.errai.cdi.injection.client.qualifier.QualV;
import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.DestructionCallback;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.IOCBeanDef;
import org.jboss.errai.ioc.client.container.SyncBeanManager;
import org.jboss.errai.ioc.client.container.IOCResolutionException;

/**
 * @author Mike Brock
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class BeanManagerIntegrationTest extends AbstractErraiCDITest {
  {
    disableBus = true;
  }

  private final QualA QUAL_A = new QualA() {
    @Override
    public Class<? extends Annotation> annotationType() {
      return QualA.class;
    }
  };

  @Override
  public String getModuleName() {
    return "org.jboss.errai.cdi.injection.InjectionTestModule";
  }

  public void testBeanManagerLookupSimple() {
    final IOCBeanDef<ApplicationScopedBean> bean = IOC.getBeanManager().lookupBean(ApplicationScopedBean.class);
    assertNotNull(bean);
  }

  public void testBeanManagerLookupInheritedScopeBean() {
    final IOCBeanDef<InheritedApplicationScopedBean> bean =
        IOC.getBeanManager().lookupBean(InheritedApplicationScopedBean.class);
    assertNotNull("inherited application scoped bean did not lookup", bean);

    final InheritedApplicationScopedBean beanInst = bean.getInstance();
    assertNotNull("bean instance is null", beanInst);

    final DependentScopedBean bean1 = beanInst.getBean1();
    assertNotNull("bean1 is null", bean1);

    final DependentScopedBeanWithDependencies beanWithDependencies = beanInst.getBeanWithDependencies();
    assertNotNull("beanWithDependencies is null", beanWithDependencies);

    final DependentScopedBean bean2 = beanWithDependencies.getBean();
    assertNotSame("bean1 and bean2 should be different", bean1, bean2);

    final InheritedApplicationScopedBean beanInst2 = bean.getInstance();
    assertSame("bean is not observing application scope", beanInst, beanInst2);
  }

  public void testBeanManagerLookupBeanFromAbstractRootType() {
    final IOCBeanDef<AbstractBean> bean = IOC.getBeanManager().lookupBean(AbstractBean.class);
    assertNotNull("did not find any beans matching", bean);

    final AbstractBean beanInst = bean.getInstance();
    assertNotNull("bean instance is null", beanInst);

    assertTrue("bean is incorrect instance: " + beanInst.getClass(), beanInst instanceof InheritedFromAbstractBean);
  }

  /**
   * This test effectively tests that the IOC container comprehends the full type hierarchy, considering both supertypes
   * and transverse interface types.
   */
  public void testBeanManagerLookupForOuterInterfaceRootType() {
    final IOCBeanDef<OuterBeanInterface> bean = IOC.getBeanManager().lookupBean(OuterBeanInterface.class);
    assertNotNull("did not find any beans matching", bean);

    final OuterBeanInterface beanInst = bean.getInstance();
    assertNotNull("bean instance is null", beanInst);

    assertTrue("bean is incorrect instance: " + beanInst.getClass(), beanInst instanceof InheritedFromAbstractBean);
  }

  public void testBeanManagerLookupForOuterInterfacesOfNonAbstractType() {
    final IOCBeanDef<InterfaceC> beanC = IOC.getBeanManager().lookupBean(InterfaceC.class);
    assertNotNull("did not find any beans matching", beanC);

    final IOCBeanDef<InterfaceD> beanD = IOC.getBeanManager().lookupBean(InterfaceD.class);
    assertNotNull("did not find any beans matching", beanD);
  }

  public void testBeanManagerLookupForExtendedInterfaceType() {
    // This should find ApplicationScopedBeanA, ApplicationScopedBeanB and ApplicationScopedBeanC
    final Collection<IOCBeanDef<InterfaceRoot>> beans = IOC.getBeanManager().lookupBeans(InterfaceRoot.class);
    assertEquals("did not find all managed implementations of " + InterfaceRoot.class.getName(), 3, beans.size());

    // This should find ApplicationScopedBeanA and ApplicationScopedBeanB (InterfaceB extends InterfaceA)
    final Collection<IOCBeanDef<InterfaceA>> beansB = IOC.getBeanManager().lookupBeans(InterfaceA.class);
    assertEquals("did not find both managed implementations of " + InterfaceA.class.getName(), 2, beansB.size());

    // This should find only ApplicationScopedBeanB
    final Collection<IOCBeanDef<InterfaceB>> beansC = IOC.getBeanManager().lookupBeans(InterfaceB.class);
    assertEquals("did not find exactly one managed implementation of " + InterfaceB.class.getName(), 1, beansC.size());
  }

  @SuppressWarnings("rawtypes")
  public void testBeanManagerLookupForGenericType() {
    final IOCBeanDef<HistoryStack> bean = IOC.getBeanManager().lookupBean(HistoryStack.class);
    assertNotNull("did not find managed bean of generic type " + HistoryStack.class.getName(), bean.getInstance());
    
    HistoryStack beanInst = bean.getInstance();
    assertNotNull("did not find injected generic bean", beanInst.getHistoryList());
  }

  public void testBeanManagerAPIs() {
    final SyncBeanManager mgr = IOC.getBeanManager();
    final IOCBeanDef<QualAppScopeBeanA> bean = mgr.lookupBean(QualAppScopeBeanA.class);

    final Set<Annotation> a = bean.getQualifiers();
    assertEquals("there should be two qualifiers", 2, a.size());
    assertTrue("wrong qualifiers", annotationSetMatches(a, QualA.class, Any.class));
  }

  public void testQualifiedLookup() {
    final QualA qualA = new QualA() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return QualA.class;
      }
    };

    final QualB qualB = new QualB() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return QualB.class;
      }
    };

    final Collection<IOCBeanDef<CommonInterface>> beans = IOC.getBeanManager().lookupBeans(CommonInterface.class);
    assertEquals("wrong number of beans", 2, beans.size());

    final IOCBeanDef<CommonInterface> beanA = IOC.getBeanManager().lookupBean(CommonInterface.class, qualA);
    assertNotNull("no bean found", beanA);
    assertTrue("wrong bean looked up", beanA.getInstance() instanceof QualAppScopeBeanA);

    final IOCBeanDef<CommonInterface> beanB = IOC.getBeanManager().lookupBean(CommonInterface.class, qualB);
    assertNotNull("no bean found", beanB);
    assertTrue("wrong bean looked up", beanB.getInstance() instanceof QualAppScopeBeanB);
  }

  public void testQualifierLookupWithAnnoAttrib() {
    final QualV qualApples = new QualV() {
      @Override
      public QualEnum value() {
        return QualEnum.APPLES;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return QualV.class;
      }

      @Override
      public int amount() {
        return 5;
      }
    };

    final QualV qualOranges = new QualV() {
      @Override
      public QualEnum value() {
        return QualEnum.ORANGES;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return QualV.class;
      }

      @Override
      public int amount() {
        return 6;
      }
    };

    final Collection<IOCBeanDef<CommonInterfaceB>> beans = IOC.getBeanManager().lookupBeans(CommonInterfaceB.class);
    assertEquals("wrong number of beans", 2, beans.size());

    final IOCBeanDef<CommonInterfaceB> beanA = IOC.getBeanManager().lookupBean(CommonInterfaceB.class, qualApples);
    assertNotNull("no bean found", beanA);
    assertTrue("wrong bean looked up", beanA.getInstance() instanceof QualParmAppScopeBeanApples);

    final IOCBeanDef<CommonInterfaceB> beanB = IOC.getBeanManager().lookupBean(CommonInterfaceB.class, qualOranges);
    assertNotNull("no bean found", beanB);
    assertTrue("wrong bean looked up", beanB.getInstance() instanceof QualParmAppScopeBeanOranges);
  }

  public void testQualifierLookupWithAnnoAttribFailure() {
    final QualV qualOrange = new QualV() {
      @Override
      public QualEnum value() {
        return QualEnum.ORANGES;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return QualV.class;
      }

      @Override
      public int amount() {
        return 5;
      }
    };

    final QualV qualApple = new QualV() {
      @Override
      public QualEnum value() {
        return QualEnum.APPLES;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return QualV.class;
      }

      @Override
      public int amount() {
        return 6;
      }
    };

    final Collection<IOCBeanDef<CommonInterfaceB>> beans = IOC.getBeanManager().lookupBeans(CommonInterfaceB.class);
    assertEquals("wrong number of beans", 2, beans.size());

    final IOCBeanDef<CommonInterfaceB> beanA = IOC.getBeanManager().lookupBean(CommonInterfaceB.class, qualOrange);
    assertNotNull("no bean found", beanA);
    assertFalse("wrong bean looked up", beanA.getInstance() instanceof QualParmAppScopeBeanApples);

    final IOCBeanDef<CommonInterfaceB> beanB = IOC.getBeanManager().lookupBean(CommonInterfaceB.class, qualApple);
    assertNotNull("no bean found", beanB);
    assertFalse("wrong bean looked up", beanB.getInstance() instanceof QualParmAppScopeBeanOranges);
  }

  public void testQualifiedLookupFailure() {
    final LincolnBar wrongAnno = new LincolnBar() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return LincolnBar.class;
      }
    };

    try {
      final IOCBeanDef<CommonInterface> bean = IOC.getBeanManager().lookupBean(CommonInterface.class);
      fail("should have thrown an exception, but got: " + bean);
    }
    catch (IOCResolutionException e) {
      assertTrue("wrong exception thrown: " + e.getMessage(), e.getMessage().contains("multiple matching"));
    }

    try {
      final IOCBeanDef<CommonInterface> bean = IOC.getBeanManager().lookupBean(CommonInterface.class, wrongAnno);
      fail("should have thrown an exception, but got: " + bean);
    }
    catch (IOCResolutionException e) {
      assertTrue("wrong exception thrown", e.getMessage().contains("no matching"));
    }
  }

  public void testLookupByName() {
    final Collection<IOCBeanDef> beans = IOC.getBeanManager().lookupBeans("animal");

    assertEquals("wrong number of beans", 2, beans.size());
    assertTrue("should contain a pig", containsInstanceOf(beans, Pig.class));
    assertTrue("should contain a cow", containsInstanceOf(beans, Cow.class));
  }

  public void testLookupAllBeans() {
    final Collection<IOCBeanDef<Object>> beans = IOC.getBeanManager().lookupBeans(Object.class);

    assertTrue(!beans.isEmpty());
  }

  public void testLookupAllBeansQualified() {
    final Collection<IOCBeanDef<Object>> beans = IOC.getBeanManager().lookupBeans(Object.class, QUAL_A);

    assertEquals(1, beans.size());
    assertEquals(QualAppScopeBeanA.class, beans.iterator().next().getBeanClass());
  }

  public void testReportedScopeCorrect() {
    final IOCBeanDef<ApplicationScopedBean> appScopeBean = IOC.getBeanManager().lookupBean(ApplicationScopedBean.class);
    final IOCBeanDef<DependentScopedBean> dependentIOCBean = IOC.getBeanManager().lookupBean(DependentScopedBean.class);

    assertEquals(ApplicationScoped.class, appScopeBean.getScope());
    assertEquals(Dependent.class, dependentIOCBean.getScope());
  }

  public void testAddingProgrammaticDestructionCallback() {
    final DependentScopedBean dependentScopedBean
        = IOC.getBeanManager().lookupBean(DependentScopedBean.class).newInstance();

    class TestValueHolder {
      boolean destroyed = false;
    }

    final TestValueHolder testValueHolder = new TestValueHolder();

    IOC.getBeanManager().addDestructionCallback(dependentScopedBean, new DestructionCallback<Object>() {
      @Override
      public void destroy(Object bean) {
        testValueHolder.destroyed = true;
      }
    });

    IOC.getBeanManager().destroyBean(dependentScopedBean);

    assertEquals(true, testValueHolder.destroyed);
  }

  /**
   * Tests that beans marked as Dependent scoped by an IOCExtension can still be forced into a different scope (in this
   * case, ApplicationScoped) when they are annotated as such.
   * <p>
   * Besides this being a good idea on its own, both Errai UI Templates and Errai Navigation rely on this behaviour.
   */
  public void testNormalScopeOverridesDependent() {
    final FoobieScopedBean foobieScopedBean1 = IOC.getBeanManager().lookupBean(FoobieScopedBean.class).getInstance();
    final FoobieScopedBean foobieScopedBean2 = IOC.getBeanManager().lookupBean(FoobieScopedBean.class).getInstance();

    assertNotNull(foobieScopedBean1);
    assertNotSame(foobieScopedBean1, foobieScopedBean2);

    final FoobieScopedOverriddenBean foobieScopedOverriddenBean1
        = IOC.getBeanManager().lookupBean(FoobieScopedOverriddenBean.class).getInstance();

    final FoobieScopedOverriddenBean foobieScopedOverriddenBean2
        = IOC.getBeanManager().lookupBean(FoobieScopedOverriddenBean.class).getInstance();

    assertNotNull(foobieScopedOverriddenBean1);
    assertSame(foobieScopedOverriddenBean1, foobieScopedOverriddenBean2);
  }


  private static boolean containsInstanceOf(final Collection<IOCBeanDef> defs, final Class<?> clazz) {
    for (final IOCBeanDef def : defs) {
      if (def.getType().equals(clazz)) return true;
    }
    return false;
  }
}