/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.kubernetes

public class EphemeralSpec extends JTEPipelineSpecification {

  def Ephemeral = null

  public static class DummyException extends RuntimeException {
		public DummyException(String _message) { super( _message ); }
	}

  def setup() {
    Ephemeral = loadPipelineScriptForStep("kubernetes","ephemeral")
    explicitlyMockPipelineVariable("out")
    explicitlyMockPipelineStep("withGit")
    explicitlyMockPipelineStep("inside_sdp_image")
    explicitlyMockPipelineStep("withKubeConfig")

    Ephemeral.getBinding().setVariable("env", [REPO_NAME: "unit-test", GIT_SHA: "abcd1234", JOB_NAME: "app/test/PR-test", BUILD_NUMBER: '7'])
    Ephemeral.getBinding().setVariable("token", "token")

    getPipelineMock("readYaml")(_ as Map) >> [
      image_shas: [
        unit_test: "efgh5678"
      ]
    ]
    getPipelineMock("sh")(_ as Map) >> "ENV:\nA:Alpha\nB:Bravo\nC:Charlie"
  }

  /*************************
   Variable Assignment Logic
  *************************/

  def "Throw error if helm_configuration_repository is not defined" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock('error')("helm_configuration_repository not defined in library config or application environment config")
  }

  def "Use the library config's helm_configuration_repository if not set in app_env" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository: null]
      Ephemeral.getBinding().setVariable("config", [helm_configuration_repository: "config_hcr"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withGit")( _ ) >> { _arguments ->
        assert _arguments[0][0].url == "config_hcr"
      }
  }

  def "Use the app_env's helm_configuration_repository if defined" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository: "app_env_hcr"]
      Ephemeral.getBinding().setVariable("config", [helm_configuration_repository: "config_hcr"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withGit")( _ ) >> { _arguments ->
        assert _arguments[0][0].url == "app_env_hcr"
      }
  }

  // helm_configuration_repository_credential (HCRC)
  def "Throw error if helm_configuration_repository_credential HCRC not defined" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository_credential: null]
      Ephemeral.getBinding().setVariable("config", [helm_configuration_repository_credential: null])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("error")("GitHub Credential For Configuration Repository Not Defined")
  }

  def "Use the github_credential if HCRC not defined in the library config or app_env" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository_credential: null]
      Ephemeral.getBinding().setVariable("config", [helm_configuration_repository_credential: null])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: "github_credential"])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withGit")( _ ) >> { _arguments ->
        assert _arguments[0][0].cred == "github_credential"
      }
  }

  def "Use the HCRC defined by the library config if not defined by the app_env" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository_credential: null]
      Ephemeral.getBinding().setVariable("config", [helm_configuration_repository_credential: "config_hcrc"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: "github_credential"])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withGit")( _ ) >> { _arguments ->
        assert _arguments[0][0].cred == "config_hcrc"
      }
  }

  def "Use the HCRC defined by the app_env if available" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository_credential: "app_env_hcrc"]
      Ephemeral.getBinding().setVariable("config", [helm_configuration_repository_credential: "config_hcrc"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: "github_credential"])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withGit")( _ ) >> { _arguments ->
        assert _arguments[0][0].cred == "app_env_hcrc"
      }
  }

  // k8s_credential
  def "Throw error if k8s_credential is not defined" () {
    setup:
      def app_env = [k8s_credential: null]
      Ephemeral.getBinding().setVariable("config", [])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("error")("k8s Credential Not Defined")
  }

  def "Use Application Environment k8s_credential if defined" () {
    setup:
      def app_env = [k8s_credential: "appenv_k8s_cred"]
      Ephemeral.getBinding().setVariable("config", [k8s_credential: "config_k8s_cred"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      2 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
            assert _arguments[0][0] == ['credentialsId':'appenv_k8s_cred', 'contextName':null]
      }
  }
  def "Use Library config k8s_credential if defined and Application Environment k8s_credential is null" () {
    setup:
      def app_env = []
      Ephemeral.getBinding().setVariable("config", [k8s_credential: "config_k8s_cred"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      2 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
            assert _arguments[0][0] == ['credentialsId':'config_k8s_cred', 'contextName':null]
      }
  }

  // k8s_context
  def "Throw error if k8s_context is not defined" () {
    setup:
      def app_env = [k8s_context: null]
      Ephemeral.getBinding().setVariable("config", [])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("error")("k8s_context Not Defined")
  }

  def "Use Application Environment k8s_context if defined" () {
    setup:
      def app_env = [k8s_context: "appenv_context"]
      Ephemeral.getBinding().setVariable("config", [k8s_context: "config_context"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      2 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
            assert _arguments[0][0] == ['credentialsId':null, 'contextName':'appenv_context']
      }
  }
  def "Use Library config k8s_context if defined and Application Environment k8s_context is null" () {
    setup:
      def app_env = []
      Ephemeral.getBinding().setVariable("config", [k8s_context: "config_context"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      2 * getPipelineMock("withKubeConfig")(_) >> {_arguments -> 
            assert _arguments[0][0] == ['credentialsId':null, 'contextName':'config_context']
      }
  }

  /**************************
   helm deploy
  ***************************/

  def "Throw error if no values file is defined" () {
    setup:
      def app_env = [short_name: null, long_name: 'Environment', chart_values_file: null]
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("error")( "Values File To Use For This Chart Not Defined" )
  }

  def "Use the short name to define the values file if app_env.chart_values_file is not set" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', chart_values_file: null]
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("sh")( "rm values.env.yaml" )
  }

  def "Use app_env.chart_values_file for the values_file if available" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', chart_values_file: "special_values_file.yaml"]
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("sh")( "rm special_values_file.yaml" )
  }
  
  def "Checkout main branch of HCR if no helm_configuration_repository_branch from app_env" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository_branch: null]
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withGit")( { (it[0] instanceof Map) ? it[0]?.branch == "main" : false} )
  }
  
  def "Checkout app_env.helm_configuration_repository_branch of HCR if defined" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', helm_configuration_repository_branch: "Mercator"]
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withGit")( { (it[0] instanceof Map) ? it[0]?.branch == "Mercator" : false} )
  }

  /**************************
   core functionality tests
  **************************/

  def "The body gets executed" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {echo "hello world"})
    then:
      1 * getPipelineMock("echo")("hello world")
  }

  //There's room for improvement for this test - KO
  def "withEnv is used to pass in the release's environment variables" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
      // environment variables set by a stub in setup()
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("withEnv")({ it[0] ? it[0] == ["A=Alpha", "B=Bravo", "C=Charlie"] : false})
  }


  /**************************
   update_values_file() tests
  ***************************/

  def "Throw error if values_file is not in the config_repo" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment', chart_values_file: "The limit of f(x) as x approaches 0"]
      Ephemeral.getBinding().setVariable("config", [helm_configuration_repository: "the equation f(x) = 1/x"])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("fileExists")("The limit of f(x) as x approaches 0") >> false
      1 * getPipelineMock("error")("Values File The limit of f(x) as x approaches 0 does not exist in the given Helm configuration repo")
  }

  def "Values file is updated with new Git SHA" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      // env.REPO_NAME and env.GIT_SHA set above in setup()
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("readYaml")([file: "values.env.yaml"]) >>  [image_shas: [unit_test : "efgh5678"]]
      1 * getPipelineMock("echo")("writing new Git SHA abcd1234 to image_shas.unit_test in values.env.yaml")
      1 * getPipelineMock("sh")("rm values.env.yaml") // remove the old file to write a new one
      1 * getPipelineMock("writeYaml")([file: "values.env.yaml", data: [image_shas: [unit_test : "abcd1234"], is_ephemeral: true]])
  }

  /*********************
   prep_project() tests
  *********************/

  def "Release name is generated correctly" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("echo")({ it =~ /Ephemeral Environment Name: unit-test-abcd1234/})
  }

  def "A new Kubernetes namespace is created with the correct name" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("sh")({ it =~ /kubectl create namespace unit-test-abcd1234/})
  }

  def "If an error is thrown during the setup process, delete the new project" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      try{
        Ephemeral(app_env, {})
      } catch ( Exception e ){}
    then:
      1 * getPipelineMock("sh")({ it =~ /kubectl create namespace unit-test-abcd1234/}) >> {throw Exception}
      (1.._) * getPipelineMock("sh")({ it =~ /kubectl delete namespace unit-test-abcd1234/ })
  }

  def "prep_project() returns the release name" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
      def retval
      def random_name
    when:
      retval = Ephemeral.prep_project("image_repo_project")
    then:
      1 * getPipelineMock("echo")({ it =~ /Ephemeral Environment Name: unit-test-abcd1234/ }) >> { _arguments ->
        random_name = _arguments[0]
      }
    expect:
      "Ephemeral Environment Name: ${retval}" == random_name
  }

  /******************
   do_release() tests
  *******************/

  def "Chart deploys with the defined release and values_file" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("sh")({it instanceof Map ? it["script"] =~ /helm upgrade --install  --namespace/ : false}) >> "ENV:\nA:Alpha\nB:Bravo\nC:Charlie"
  }

  def "do_release() returns the list of variables relevant to the new release/environment" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      def retval = Ephemeral.do_release("abcdefghij", "values.env.yaml")
    then:
      retval?.A == "Alpha"
      retval?.B == "Bravo"
      retval?.C == "Charlie"

  }

  /****************
   cleanup() tests
  ****************/

  def "The ephemeral environment's release is purged from Kubernetes" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("sh")({it =~ /helm del --purge unit-test-abcd1234/})
  }

  def "The ephemeral environment's release namespace is deleted from Kubernetes" () {
    setup:
      def app_env = [short_name: 'env', long_name: 'Environment']
      Ephemeral.getBinding().setVariable("config", [:])
      Ephemeral.getBinding().setVariable("pipelineConfig", [github_credential: null])
    when:
      Ephemeral(app_env, {})
    then:
      1 * getPipelineMock("sh")({it =~ /kubectl delete namespace unit-test-abcd1234/})
  }
  
}
