@gradle_demo
Feature: Demo of wrong sandbox directory
  Intellij gradle plugin 1.1.3 is used due to https://github.com/JetBrains/gradle-intellij-plugin/issues/752
  Prerequisites:
  1. install `cucumber for java' IDEA plugin
  2. checkout `experimental/gradle_issue` branch, project https://github.com/JetBrains-Research/snakecharm/
  3. create new project from existing sources in IDEA, choose import from gradle option
  4. Ok sandbox example
     - Run 'tests' gradle task
     Sandbox is expected directory according to debug log:
       Caches: /Users/romeo/work/git_repos/snakecharm_related/snakecharm_new/.sandbox_pycharm/system-test/index
  5. Wrong sanbox example (any of 2 options):
     - Open `gradle_issue.feature` file and run it via context menu (will be launched using cucumber)
     - Run `cucumber_gradle_demo` gradle task
     In all cases test process output has debug output:
        `Caches: /Users/romeo/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.pycharm/pycharmPC/2021.1.1/7e006ed9da260f8a5bf071cb7523e125f956f18a/pycharmPC-2021.1.1/system/index`
     Build step debug log includes:
        > Configure project :
        Plugin sandbox: /Users/romeo/work/git_repos/snakecharm_related/snakecharm_new/.sandbox_pycharm, property: extension 'intellij' property 'sandboxDir'
  Finally: 1) sandbox is wrong in some cases 2) seems customised `ideaDependencyCachePath` is ignored.

  Scenario: Demo test
    Given a snakemake:5x project
    Given I open a file "foo.smk" with text
    """
    rules
    """
    When I put the caret at ru
    Then reference should resolve to "rules" in "workflow.py"
