/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.anchore

public class AddRegistryCredsSpec extends JTEPipelineSpecification {

  def AddRegistryCreds = null
	def app_env = null
	def fullConfig = [
		cred: "test-cred",
		docker_registry_credential_id: "config_docker_registry",
		docker_registry_name: "config_docker_reg",
		k8s_credential: "config_k8s_credential",
		k8s_context: "config_k8s_context",
		anchore_engine_url: "config_anchore_engine_url"
	]

  def setup() {
    app_env = [anchore: [:]]
    AddRegistryCreds = loadPipelineScriptForStep("anchore","add_registry_creds")
    AddRegistryCreds.getBinding().setVariable("config", [:])
		AddRegistryCreds.getBinding().setVariable("env", [ANCHORE_ENGINE_URL: null])

		explicitlyMockPipelineVariable("out")
		explicitlyMockPipelineVariable("usernamePassword")
    explicitlyMockPipelineVariable("ANCHORE_ENGINE_URL")
    explicitlyMockPipelineVariable("ANCHORE_USERNAME")
    explicitlyMockPipelineVariable("ANCHORE_PASSWORD")
    explicitlyMockPipelineVariable("REGISTRY_USERNAME")
    explicitlyMockPipelineVariable("REGISTRY_PASSWORD")
    explicitlyMockPipelineStep("inside_sdp_image")
    explicitlyMockPipelineStep("withKubeConfig")
  }

  def "Fails when required inputs are not provided" () {
    when:
      AddRegistryCreds()
    then:
      1 * getPipelineMock("error")("Kubernetes Credential Not Defined")
      1 * getPipelineMock("error")("Kubernetes Context Not Defined")
      1 * getPipelineMock("error")("Anchore Engine Url Not Defined")
  }

  def "Uses Application Environment config when available" () {
		setup:
			app_env = [anchore: [
				docker_registry_credential_id: "appenv_docker_registry",
				docker_registry_name: "appenv_docker_reg",
				k8s_credential: "appenv_k8s_credential",
				k8s_context: "appenv_k8s_context",
				anchore_engine_url: "appenv_anchore_engine_url"
			]]
			AddRegistryCreds.getBinding().setVariable("config", fullConfig)
    when:
      AddRegistryCreds(app_env)
    then:
      0 * getPipelineMock("error")("Kubernetes Credential Not Defined")
      0 * getPipelineMock("error")("Kubernetes Context Not Defined")
      0 * getPipelineMock("error")("Anchore Engine Url Not Defined")
			1 * getPipelineMock("usernamePassword.call")([credentialsId:app_env.anchore.docker_registry_credential_id, usernameVariable:'REGISTRY_USERNAME', passwordVariable:'REGISTRY_PASSWORD'])
			1 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
            assert _arguments[0][0].credentialsId == app_env.anchore.k8s_credential
            assert _arguments[0][0].contextName == app_env.anchore.k8s_context
      }
			1 * getPipelineMock("sh")({it =~ /curl --header.*appenv_anchore_engine_url.*appenv_docker_reg.*/})
  }

  def "Uses Library config when Application Environment config not available" () {
		setup:
			AddRegistryCreds.getBinding().setVariable("config", fullConfig)
    when:
      AddRegistryCreds()
    then:
      0 * getPipelineMock("error")("Kubernetes Credential Not Defined")
      0 * getPipelineMock("error")("Kubernetes Context Not Defined")
      0 * getPipelineMock("error")("Anchore Engine Url Not Defined")
			1 * getPipelineMock("usernamePassword.call")([credentialsId:fullConfig.docker_registry_credential_id, usernameVariable:'REGISTRY_USERNAME', passwordVariable:'REGISTRY_PASSWORD'])
			1 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
            assert _arguments[0][0].credentialsId == fullConfig.k8s_credential
            assert _arguments[0][0].contextName == fullConfig.k8s_context
      }
			1 * getPipelineMock("sh")({it =~ /curl --header.*config_anchore_engine_url.*config_docker_reg.*/})
  }

}