/*
 * Copyright 2012 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.cdi.async.test.cyclic.client.res;

import org.jboss.errai.ioc.client.api.LoadAsync;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Mike Brock
 */
@ApplicationScoped @LoadAsync
public class BeanInjectSelf {
  private static int counter = 0;
  private int instance = ++counter;
  private BeanInjectSelf self;

  // required to make proxyable
  public BeanInjectSelf() {
  }

  // it makes me angry that I actually had to support this to be consistent with the JSR-299 TCK.
  // in fact, I find it absurd that I'm not throwing an exception right now.
  @Inject
  public BeanInjectSelf(BeanInjectSelf selfRefProxy) {
    this.self = selfRefProxy;
  }

  public int getInstance() {
    return instance;
  }

  public BeanInjectSelf getSelf() {
    return self;
  }
}
