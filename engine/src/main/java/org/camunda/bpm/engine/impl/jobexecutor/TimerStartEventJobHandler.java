/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.jobexecutor;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.ProcessInstantiationBuilderImpl;
import org.camunda.bpm.engine.impl.cmd.StartProcessInstanceCmd;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.deploy.DeploymentCache;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstantiationBuilder;


public class TimerStartEventJobHandler extends TimerEventJobHandler {

  private final static JobExecutorLogger LOG = ProcessEngineLogger.JOB_EXECUTOR_LOGGER;

  public static final String TYPE = "timer-start-event";

  public String getType() {
    return TYPE;
  }

  public void execute(String configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    DeploymentCache deploymentCache = Context
            .getProcessEngineConfiguration()
            .getDeploymentCache();

    String definitionKey = getKey(configuration);
    ProcessDefinition processDefinition = deploymentCache.findDeployedLatestProcessDefinitionByKeyAndTenantId(definitionKey, tenantId);
    try {
      if(!processDefinition.isSuspended()) {
        RuntimeService runtimeService = commandContext.getProcessEngineConfiguration().getRuntimeService();
        // TODO use the method from runtime service - CAM-5211
        ProcessInstantiationBuilder processInstantiationBuilder = runtimeService.createProcessInstanceByKey(definitionKey);
        new StartProcessInstanceCmd((ProcessInstantiationBuilderImpl) processInstantiationBuilder, tenantId).execute(commandContext);
      }
      else {
        LOG.ignoringSuspendedJob(processDefinition);
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new ProcessEngineException("exception during timer execution: " + e.getMessage(), e);
    }
  }
}
