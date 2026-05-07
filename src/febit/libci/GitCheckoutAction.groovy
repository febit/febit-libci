package febit.libci

import org.febit.libci.core.predefined.ScmPredefined
import org.febit.libci.core.predefined.git.GitCommitField
import org.febit.libci.extern.GitCommitLogFormat

class GitCheckoutAction {

    private final LibciContext ctx

    GitCheckoutAction(LibciContext ctx) {
        this.ctx = ctx
    }

    void run() {
        ctx.stage('Checkout') {
            checkout()
            detectLastCommit()
        }
    }

    private void detectLastCommit() {
        def out = ctx.sh(
            label: 'Detect last commit',
            returnStdout: true,
            script: """\
git log \\
  --no-color \\
  --max-count=1 \\
  --pretty=format:'${GitCommitLogFormat.format(GitCommitField.values())}'
"""
        ) as String
        def props = GitCommitLogFormat.parseLog(out)
        ScmPredefined.commit(ctx.vars.predefined, props)
    }

    private void checkout() {
        def scm = ctx.conf.scm
        def args = [
            $class           : 'GitSCM',
            userRemoteConfigs: [
                [
                    name: 'origin',
                    url          : scm.url,
                    credentialsId: scm.credentialsId,
                ],
            ],
            branches         : [
                [name: scm.commitId ?: scm.ref]
            ],
            extensions       : [],
        ]

        // Submodule support
        args += [
            doGenerateSubmoduleConfigurations: false,
            submoduleCfg                     : [],
        ]
        args['extensions'] += [
            $class             : 'SubmoduleOption',
            reference          : '',
            parentCredentials  : true,
            disableSubmodules  : false,
            trackingSubmodules : false,
            recursiveSubmodules: true,
        ]
        ctx.runtime.checkout(args)
    }
}

