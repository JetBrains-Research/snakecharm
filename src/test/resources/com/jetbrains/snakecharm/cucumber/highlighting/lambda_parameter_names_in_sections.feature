Feature: Inspection checking lambda parameter names in various sections

  Scenario Outline: 'wildcards' parameter in input section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> bwa_map:
      input:
        "data/genome.fa",
        lambda wildcards: config["samples"][wildcards.sample]
      output:
        "output.txt"
    """
    And Lambda Parameter Names inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Incorrect lambda parameter in input section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input:
        "input.txt",
        lambda a: a + "addition"
      output:
        "output.txt"
    """
    And Lambda Parameter Names inspection is enabled
    Then I expect inspection error on <a> in <a:> with message
    """
    Only use 'wildcards' as lambda parameter in 'input' section.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Too many lambda parameters in input section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input:
        "input.txt",
        lambda wildcards, a: a + "addition"
      output:
        "output.txt"
    """
    And Lambda Parameter Names inspection is enabled
    Then I expect inspection error on <a> in <, a:> with messages
      | Only use 'wildcards' as lambda parameter in 'input' section. |
      | Don't use more than 1 lambda parameters in 'input' section.  |

    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Correct lambda parameters in params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      params:
        prefix=lambda wildcards, output: output[0][:-4]
      output:
        "somedir/{sample}.csv"
      shell:
        "somecommand -o {params.prefix}"
    """
    And Lambda Parameter Names inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Incorrect lambda parameter in params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      params:
        prefix=lambda wildcards, output, input, resources, log: output[0][:-4]
      output:
        "somedir/{sample}.csv"
      shell:
        "somecommand -o {params.prefix}"
    """
    And Lambda Parameter Names inspection is enabled
    Then I expect inspection error on <log> with message
    """
    Only use 'wildcards/input/output/resources/threads' as lambda parameter in 'params' section.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Incorrect lambda parameters order in params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      params:
        prefix=lambda output, wildcards, input, resources, threads: output[0][:-4]
      shell:
        "somecommand -o {params.prefix}"
    """
    And Lambda Parameter Names inspection is enabled
    Then I expect inspection error on <output> with message
    """
    'wildcards' has to be the first lambda parameter.
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Too many lambda parameters in params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      params:
        prefix=lambda wildcards, output, input, resources, threads, log: output[0][:-4]
      shell:
        "somecommand -o {params.prefix}"
    """
    And Lambda Parameter Names inspection is enabled
    Then I expect inspection error on <log> with messages
      | Only use 'wildcards/input/output/resources/threads' as lambda parameter in 'params' section. |
      | Don't use more than 5 lambda parameters in 'params' section.                                 |

    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |