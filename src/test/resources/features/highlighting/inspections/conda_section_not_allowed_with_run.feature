Feature: Inspection warns about conda section with run section.

  Scenario Outline: Conda not allowed with run
    Given a snakemake project
    And a file "env.yaml" with text
    """
    """
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
      conda: "env.yaml" 
      run: pass
    """
    When SmkCondaSectionNotAllowedWithRun inspection is enabled
    Then I expect inspection error on <conda: "env.yaml"> with message
    """
    Conda environments are not allowed with run directive.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No error if with other execution sections
    Given a snakemake project
    And a file "env.yaml" with text
    """
    """
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
      conda: "env.yaml" 
      <section_text>
    """
    When SmkCondaSectionNotAllowedWithRun inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  | section_text  |
      | rule       | shell: "cmd"  |
      | checkpoint | shell: "cmd"  |
      | rule       | script: "s"   |
      | rule       | wrapper: "w"  |
      | rule       | notebook: "n" |