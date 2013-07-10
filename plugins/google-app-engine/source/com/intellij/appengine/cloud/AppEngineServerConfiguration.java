/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.appengine.cloud;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class AppEngineServerConfiguration extends ServerConfiguration implements PersistentStateComponent<AppEngineServerConfiguration> {
  private String myEmail;

  @Attribute("email")
  public String getEmail() {
    return myEmail;
  }

  public void setEmail(String email) {
    myEmail = email;
  }

  @Nullable
  @Override
  public AppEngineServerConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(AppEngineServerConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Override
  public PersistentStateComponent<?> getSerializer() {
    return this;
  }
}
