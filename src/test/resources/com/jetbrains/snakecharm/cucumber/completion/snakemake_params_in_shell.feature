Feature: Completion for params in shell section
  Complete params section arguments in shell section

  Scenario Outline: Complete in shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin))
      shell: "command --workdir {params.outdir}"
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list should contain:
      | outdir      |
      | xmx         |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |


  Scenario Outline: Not completed in wrapper section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin))
      wrapper: "wrapper {params.outdir}"
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | outdir      |
      | xmx         |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: no values for unnamed parameters in the completion list
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        1,
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin))
      shell: "command {params.outdir}"
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list should contain:
      | outdir      |
      | xmx         |
    And completion list shouldn't contain:
      | 1           |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: Completed in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      params: 0, a=1, b=2
      run:
        shell("echo {params.a}")
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list should contain:
      | a      |
      | b      |
    And completion list shouldn't contain:
      | 0      |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |

  Scenario Outline: Not completed in calls to other functions in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      params: 0, a=1, b=2
      run:
        wrapper("path/to/wrapper{params.a}.py")
    """
    When I put the caret after {params.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | a      |
      | b      |
      | 0      |
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |
