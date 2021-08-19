Feature: Resolve after pep.config

  Scenario Outline: Resolve at toplevel
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.<key>
    """
    When I put the caret after pep.config.
    Then reference should resolve to "<key>" in "config.yaml"
    Examples:
      | key         |
      | custom_key1 |
      | custom_key2 |

  Scenario Outline: Resolve inside rule
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    rule Name:
      <section>:
        pep.config.<key>
    """
    When I put the caret after pep.config.
    Then reference should resolve to "<key>" in "config.yaml"
    Examples:
      | key         | section |
      | custom_key1 | section |
      | custom_key2 | run     |

  Scenario Outline: Resolve in "onstart" section
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    onstart :
           pep.config.<key>"
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then reference should resolve to "<key>" in "config.yaml"
    Examples:
      | key         |
      | custom_key1 |
      | custom_key2 |

  Scenario Outline: Resolve in injections
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <rule_like> NAME:
           <section>: "{pep.config.<key>}"
    """
    When I put the caret after "{pep.config.
    Then reference in injection should resolve to "<key>" in "config.yaml"
    Examples:
      | rule_like  | section | key         |
      | rule       | shell   | custom_key1 |
      | rule       | message | custom_key1 |
      | checkpoint | shell   | custom_key2 |

  Scenario Outline: No resolve in injections for wildcards expanding/defining sections
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
        <rule_like> NAME:
           <section>: "{pep.config.<key>}"
        """
    When I put the caret after "{pep.config.
    Then reference in injection should not resolve
    Examples:
      | rule_like  | section | key         |
      | rule       | input   | custom_key1 |
      | rule       | output  | custom_key1 |
      | rule       | log     | custom_key2 |
      | checkpoint | input   | custom_key2 |
