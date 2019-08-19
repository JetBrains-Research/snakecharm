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
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect no inspection weak warning
    When I check highlighting weak warnings
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
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection weak warning on <a> in <a:> with message
    """
    Snakemake documentation suggests it's preferable to name the first parameter 'wildcards'.
    """
    When I check highlighting weak warnings
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
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <a> in <, a:> with messages
      | Only use 'wildcards' as lambda parameter in 'input' section. |
      | Don't use more than 1 lambda parameter(s) in 'input' section.  |

    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: 'wildcards' parameter in group section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> bwa_map:
      group:
        lambda wildcards: config["samples"][wildcards.sample]
      output:
        "output.txt"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect no inspection weak warning
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Incorrect lambda parameter in group section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      group:
        lambda a: a + "addition"
      output:
        "output.txt"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection weak warning on <a> in <a:> with message
    """
    Snakemake documentation suggests it's preferable to name the first parameter 'wildcards'.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Too many lambda parameters in group section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      group:
        lambda wildcards, a: a + "addition"
      output:
        "output.txt"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <a> in <, a:> with messages
      | Only use 'wildcards' as lambda parameter in 'group' section. |
      | Don't use more than 1 lambda parameter(s) in 'group' section.  |
    When I check highlighting weak warnings
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
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect no inspection weak warning
    When I check highlighting weak warnings
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
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <log> with message
    """
    Only use 'wildcards/input/output/resources/threads' as lambda parameter in 'params' section.
    """
    When I check highlighting weak warnings
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
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <wildcards> with message
    """
    'wildcards' has to be the first lambda parameter.
    """
    And I expect inspection error on <output> with message
    """
    'output' cannot be the first lambda parameter in 'params' section.
    """
    When I check highlighting weak warnings
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
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <log> with messages
      | Only use 'wildcards/input/output/resources/threads' as lambda parameter in 'params' section. |
      | Don't use more than 5 lambda parameter(s) in 'params' section.                                 |

    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Correct lambda parameters in resources section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      resources:
        prefix=lambda wildcards, input, threads, attempt: attempt * 100
      output:
        "somedir/{sample}.csv"
      shell:
        "somecommand -o {resources.prefix}"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect no inspection weak warning
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Incorrect lambda parameter in resources section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      resources:
        prefix=lambda wildcards, input, threads, params: threads * 100
      output:
        "somedir/{sample}.csv"
      shell:
        "somecommand -o {resources.prefix}"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <params> with message
    """
    Only use 'wildcards/input/threads/attempt' as lambda parameter in 'resources' section.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Incorrect lambda parameters order in resources section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      resources:
        prefix=lambda threads, wildcards, input, attempt: attempt * 100
      shell:
        "somecommand -o {resources.prefix}"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <wildcards> with message
    """
    'wildcards' has to be the first lambda parameter.
    """
    And I expect inspection error on <threads> with message
    """
    'threads' cannot be the first lambda parameter in 'resources' section.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Too many lambda parameters in resources section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      resources:
        prefix=lambda wildcards, input, threads, attempt, random_name: random_name * 100
      shell:
        "somecommand -o {resources.prefix}"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <random_name> with messages
      | Don't use more than 4 lambda parameter(s) in 'resources' section.                      |
      | Only use 'wildcards/input/threads/attempt' as lambda parameter in 'resources' section. |

    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Correct lambda parameters in threads section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      threads:
        lambda wildcards, input, attempt: 10
      output:
        "somedir/{sample}.csv"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect no inspection weak warning
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Incorrect lambda parameter in threads section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      threads:
        lambda wildcards, output: 10
      output:
        "somedir/{sample}.csv"
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <output> with message
    """
    Only use 'wildcards/input/attempt' as lambda parameter in 'threads' section.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: Incorrect lambda parameters order in threads section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      threads:
        lambda input, wildcards: 10
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <wildcards> with message
    """
    'wildcards' has to be the first lambda parameter.
    """
    And I expect inspection error on <input> in <lambda input> with message
    """
    'input' cannot be the first lambda parameter in 'threads' section.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Too many lambda parameters in threads section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "input.txt"
      threads:
        lambda wildcards, input, attempt, params: attempt % 8
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <params> with messages
      | Don't use more than 3 lambda parameter(s) in 'threads' section.      |
      | Only use 'wildcards/input/attempt' as lambda parameter in 'threads' section. |
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
    
  Scenario Outline: lambda functions not allowed in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: lambda wildcards: wildcards
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect inspection error on <lambda wildcards: wildcards> with message
    """
    Callables are not allowed in '<section>' section.
    """
    When I check highlighting weak warnings
    Examples:
      | section              | rule_like  |
      | benchmark            | rule       |
      | conda                | rule       |
      | output               | rule       |
      | log                  | rule       |
      | singularity          | rule       |
      | priority             | rule       |
      | wildcard_constraints | rule       |
      | shell                | rule       |
      | wrapper              | rule       |
      | script               | rule       |
      | cwl                  | rule       |
      | version              | rule       |
      | benchmark            | checkpoint |
      | conda                | checkpoint |
      | output               | checkpoint |
      | log                  | checkpoint |
      | singularity          | checkpoint |
      | priority             | checkpoint |
      | wildcard_constraints | checkpoint |
      | shell                | checkpoint |
      | wrapper              | checkpoint |
      | script               | checkpoint |
      | cwl                  | checkpoint |
      | version              | checkpoint |

  Scenario Outline: lambda invocations in sections where lambdas are not allowed
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: (lambda wildcards: wildcards)("")
    """
    And Lambda Functions in Rule Sections inspection is enabled
    Then I expect no inspection error
    When I check highlighting weak warnings
    Examples:
      | section              | rule_like  |
      | benchmark            | rule       |
      | conda                | rule       |
      | output               | rule       |
      | log                  | rule       |
      | singularity          | rule       |
      | priority             | rule       |
      | wildcard_constraints | rule       |
      | shell                | rule       |
      | wrapper              | rule       |
      | script               | rule       |
      | cwl                  | rule       |
      | version              | rule       |
      | benchmark            | checkpoint |
      | conda                | checkpoint |
      | output               | checkpoint |
      | log                  | checkpoint |
      | singularity          | checkpoint |
      | priority             | checkpoint |
      | wildcard_constraints | checkpoint |
      | shell                | checkpoint |
      | wrapper              | checkpoint |
      | script               | checkpoint |
      | cwl                  | checkpoint |
      | version              | checkpoint |
