/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.kubernetes

public class CreateRegistrySecretSpec extends JTEPipelineSpecification {

  def CreateRegistrySecret = null
	def app_env = null
	def fullConfig = [
		k8s_credential: "config_k8s_credential",
		k8s_context: "config_k8s_context",
		docker_registry_cred: "config_docker_registry",
		docker_registry_addr: "config_docker_reg",
		release_namespace: "config_release_namespace",
		deployment_image_pull_secret_name: "config_registry"
	]

  def setup() {
    app_env = [:]
    CreateRegistrySecret = loadPipelineScriptForStep("kubernetes","create_registry_secret")
    CreateRegistrySecret.getBinding().setVariable("config", [:])
    CreateRegistrySecret.getBinding().setVariable("env", [ANCHORE_ENGINE_URL: null])

    explicitlyMockPipelineVariable("out")
    explicitlyMockPipelineVariable("usernamePassword")
    explicitlyMockPipelineVariable("DOCKER_USERNAME")
    explicitlyMockPipelineVariable("DOCKER_PASSWORD")
    explicitlyMockPipelineStep("inside_sdp_image")
    explicitlyMockPipelineStep("withKubeConfig")
  }

  def "Fails when required inputs are not provided" () {
    when:
      CreateRegistrySecret()
    then:
      1 * getPipelineMock("error")("Kubernetes Credential Not Defined")
      1 * getPipelineMock("error")("Kubernetes Context Not Defined")
      1 * getPipelineMock("error")("Docker Registry Credential Not Defined")
      1 * getPipelineMock("error")("Docker Registry Address Not Defined")
      1 * getPipelineMock("sh")({it =~ /kubectl create secret docker-registry registry -n default --docker-server=null.*/})
  }

  def "Uses Application Environment config when available" () {
		setup:
			app_env = [
				k8s_credential: "app_env_k8s_credential",
				k8s_context: "app_env_k8s_context",
				docker_registry_cred: "app_env_docker_registry",
				docker_registry_addr: "app_env_docker_reg",
				release_namespace: "app_env_release_namespace",
				deployment_image_pull_secret_name: "app_env_registry"
			]
			CreateRegistrySecret.getBinding().setVariable("config", fullConfig)
    when:
      CreateRegistrySecret(app_env)
    then:
      0 * getPipelineMock("error")("Kubernetes Credential Not Defined")
      0 * getPipelineMock("error")("Kubernetes Context Not Defined")
      0 * getPipelineMock("error")("Docker Registry Credential Not Defined")
      0 * getPipelineMock("error")("Docker Registry Address Not Defined")
			1 * getPipelineMock("usernamePassword.call")(_) >> {_arguments -> 
				assert _arguments[0].credentialsId == app_env.docker_registry_cred
			}
			1 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
				assert _arguments[0][0].credentialsId == app_env.k8s_credential
				assert _arguments[0][0].contextName == app_env.k8s_context
				}
			1 * getPipelineMock("sh")({it =~ /kubectl create secret docker-registry app_env_registry -n app_env_release_namespace --docker-server=app_env_docker_reg.*/})
  }

  def "Uses Library config when Application Environment config not available" () {
		setup:
			CreateRegistrySecret.getBinding().setVariable("config", fullConfig)
    when:
      CreateRegistrySecret()
    then:
      0 * getPipelineMock("error")("Kubernetes Credential Not Defined")
      0 * getPipelineMock("error")("Kubernetes Context Not Defined")
      0 * getPipelineMock("error")("Docker Registry Credential Not Defined")
      0 * getPipelineMock("error")("Docker Registry Address Not Defined")
			1 * getPipelineMock("usernamePassword.call")(_) >> {_arguments -> 
				assert _arguments[0].credentialsId == fullConfig.docker_registry_cred
			}
			1 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
				assert _arguments[0][0].credentialsId == fullConfig.k8s_credential
				assert _arguments[0][0].contextName == fullConfig.k8s_context
				}
			1 * getPipelineMock("sh")({it =~ /kubectl create secret docker-registry config_registry -n config_release_namespace --docker-server=config_docker_reg.*/})
  }

}