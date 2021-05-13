/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.anchore.steps

import groovy.json.*

void call(){
    
    def generate_post_data(registry, registry_user, registry_password){
        return JsonOutput.toJson([
                registry: "$registry",
                registry_name: "$registry",
                registry_type: 'docker_v2',
                registry_user: "$registry_user",
                registry_verify: true,
                registry_pass: "$registry_password"
            ])
    }

    stage("Ensure Anchore has Docker Creds"){

        def docker_registry_credential_id = config.docker_registry_credential_id ?: "docker_registry"
        def docker_registry_name = config.docker_registry_name ?: ""

        node{

            try {
                withCredentials([
                    usernamePassword(credentialsId: config.cred, usernameVariable: 'ANCHORE_USERNAME', passwordVariable: "ANCHORE_PASSWORD"),
                    usernamePassword(credentialsId: docker_registry_credential_id, usernameVariable: 'REGISTRY_USERNAME', passwordVariable: "REGISTRY_PASSWORD")
                ]) {
                    inside_sdp_image "helm", { 
                        withKubeConfig([credentialsId: "default-kubeconfig" , contextName: "default"]) {
                            sh "curl --header \"Content-Type: application/json\" -X POST -u $ANCHORE_USERNAME:$ANCHORE_PASSWORD $config.anchore_engine_url/registries -d '${generate_post_data(docker_registry_name, REGISTRY_USERNAME, REGISTRY_PASSWORD)}'"

                            sh 'curl -u $ANCHORE_USERNAME:$ANCHORE_PASSWORD $config.anchore_engine_url/registries'
                        }
                    }
                }
            } catch (e) {
                println "anchore add_registry_creds step failed: $e"
            }
        }
    }
}