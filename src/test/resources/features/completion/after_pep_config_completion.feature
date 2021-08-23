Feature: Completion after pep.config

  Scenario: Complete at toplevel
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value
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

  Scenario: Not complete no variable keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1.custom_key2: value
    custom_key3:custom_key4:
        custom_key5:
          value
    custom_key6: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | custom_key1.custom_key2 |
      | custom_key3:custom_key4 |
      | custom_key1             |
      | custom_key3             |

  Scenario: Complete pep_version
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    pep_version: "2.0.0"
    custom_key: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | pep_version |
      | custom_key  |

  Scenario: Complete pep_version should be even if the section is missing
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | pep_version |
      | custom_key  |

  Scenario Outline: Complete text keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <text_key>: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list should contain:
      | <text_key>  |
      | pep_version |
    Examples:
      | text_key        |
      | sample_table    |
      | subsample_table |

  Scenario Outline: Not complete text keys as mapping
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <text_key>:
      key1:
        value
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
    Then completion list shouldn't contain:
      | <text_key> |
      | custom_key |
    Examples:
      | text_key        |
      | pep_version     |
      | sample_table    |
      | subsample_table |

  Scenario Outline: Complete mapping keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <mapping_key>:
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
    Then completion list should contain:
      | <mapping_key> |
      | pep_version   |
    Examples:
      | mapping_key       |
      | sample_modifiers  |
      | project_modifiers |
      | custom_key        |

  Scenario Outline: Not complete mapping keys as text
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <mapping_key>: value
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
    Then completion list shouldn't contain:
      | <mapping_key> |
    Examples:
      | mapping_key       |
      | sample_modifiers  |
      | project_modifiers |


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