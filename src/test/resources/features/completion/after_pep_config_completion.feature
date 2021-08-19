Feature: Completion after pep.config

  Scenario: Complete at toplevel
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |


  Scenario: Not complete mapping keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1:
     custom_key2:
      value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | custom_key1             |
      | custom_key2             |
      | custom_key1:custom_key2 |

  Scenario: Complete key before dot
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1.custom_key2: value
    custom_key3: value
    """
      Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key3 |
    And completion list shouldn't contain:
      | custom_key1.custom_key2 |


  Scenario Outline: Complete inside rule
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
        pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |
    Examples:
      | section |
      | section |
      | run     |

  Scenario Outline: Complete in injections
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
           <section>: "{pep.config.}"
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |
    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | rule       | message |
      | checkpoint | shell   |

  Scenario: Complete in "onstart" section
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
           pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |

  Scenario Outline: No completion in injections for wildcards expanding/defining sections
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
           <section>: "{pep.config.}"
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | custom_key1 |
      | custom_key2 |
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | output  |
      | rule       | log     |
      | checkpoint | input   |

  Scenario: Complete in not-empty context
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.c
    """
    When I put the caret after pep.config.c
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |