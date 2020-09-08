Feature: Fixes PyShadowingBuiltinsInspection related false positives
  Issue #133

  @ignore("Is disabled dut to a workaround for #133")
  Scenario: PyShadowingBuiltinsInspection works in snakemake files
     Given a snakemake project
     Given I open a file "foo.smk" with text
     """
     input = 1
     """
     And PyShadowingBuiltinsInspection inspection is enabled
     Then I expect inspection weak warning on <input> with message
     """
     Shadows built-in name 'input'
     """
     When I check highlighting weak warnings
    
  Scenario Outline: Lambda params do not shadow builtin names
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule_133:
        input:  "in"
        params:
            methylomes2=lambda wildcards, input: "",
    """
    And PyShadowingBuiltinsInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |