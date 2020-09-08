Feature: Fixes PyShadowingNamesInspection related false positives
  Issue #133

  Scenario: PyShadowingNamesInspection works in snakemake files
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    resources = 1
    wd = 1
    rule rule_133:
        input:  "in"
        output: "out.txt"
        threads: 5
        params:
            methylomes3=lambda wd, resources: "",
    """
    And PyShadowingNamesInspection inspection is enabled
    # warning only for resources and wd because they == 1, not for input, output, threads
    Then I expect inspection weak warning on <wd> in <wd,> with message
    """
    Shadows name 'wd' from outer scope
    """
    Then I expect inspection weak warning on <resources> in <resources:> with message
    """
    Shadows name 'resources' from outer scope
    """

    When I check highlighting weak warnings

  Scenario Outline: Lambda params do not shadow section names
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule_133:
        input:  "in"
        output: "out.txt"
        threads: 5
        params:
            methylomes1=lambda wildcards, output: "",
            methylomes2=lambda wildcards, input: "",
            methylomes3=lambda wildcards, resources: "",
            methylomes4=lambda wildcards, threads: ""
    """
    And PyShadowingNamesInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |