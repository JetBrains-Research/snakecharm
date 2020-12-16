Feature: Inspection warns about envmodules section with run section.

  Scenario Outline: envmodules not allowed with run
    Given a snakemake project
    And a file "env.yaml" with text
    """
    """
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
      envmodules: "fastqc"
      run: pass
    """
    When SmkEnvmodulesNotAllowedSectionInspection inspection is enabled
    Then I expect inspection error on <envmodules: "fastqc"> with message
    """
    The directive 'envmodules' is only allowed with 'shell', 'script', 'notebook', or 'wrapper' directives (not with run).
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
      envmodules: "fastqc"
      <section_text>
    """
    When SmkEnvmodulesNotAllowedSectionInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  | section_text  |
      | rule       | shell: "cmd"  |
      | checkpoint | shell: "cmd"  |
      | rule       | script: "s"   |
      | rule       | wrapper: "w"  |
      | rule       | notebook: "n" |