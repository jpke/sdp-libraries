/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.kubernetes.steps

void call(app_env = []){

		// validation repeated from deploy_to to allow for standalone use. todo, see if these can be abstracted and consolidated into one.
		/*
       k8s credential with kubeconfig
    */
    def k8s_credential = app_env.k8s_credential   ?:
                            config.k8s_credential ?:
                            {error "Kubernetes Credential Not Defined"}()
    /*
       k8s context
    */
    def k8s_context = app_env.k8s_context ?:
                  config.k8s_context      ?:
                  {error "Kubernetes Context Not Defined"}()

    /*
       docker cred
    */
		def docker_registry_cred = app_env.docker_registry_cred ?:
                               config.docker_registry_cred  ?:
															 {error "Docker Registry Credential Not Defined"}()
    /*
       docker registry address
    */
		def docker_registry_addr = app_env.docker_registry_addr ?:
                               config.docker_registry_addr  ?:
															 {error "Docker Registry Address Not Defined"}()
    /*
       namespace for secret
    */
		def deployment_namespace = app_env.deployment_namespace ?:
										config.deployment_namespace  ?:
										"default"
    /*
       name of secret
    */
		def secret_name = app_env.deployment_image_pull_secret_name ?:
											config.deployment_image_pull_secret_name  ?:
											"registry"

  inside_sdp_image "helm", {
			withKubeConfig([credentialsId: k8s_credential , contextName: k8s_context]) {
          try {
              withCredentials([usernamePassword(credentialsId: docker_registry_cred, usernameVariable: 'DOCKER_USERNAME', passwordVariable: "DOCKER_PASSWORD")]) {
                  sh "kubectl create secret docker-registry $secret_name -n $deployment_namespace --docker-server=$docker_registry_addr --docker-username=$DOCKER_USERNAME --docker-password=$DOCKER_PASSWORD"
              }
          } catch (e) {
              println "Secret not created: $e"
          }
      }
  }
}