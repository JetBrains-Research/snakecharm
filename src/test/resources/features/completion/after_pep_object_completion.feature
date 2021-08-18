Feature: Completion after pep object

  Scenario: Complete at toplevel
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    pep.
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | amendments          |
      | list_amendments     |
      | config              |
      | config_file         |
      | samples             |
      | sample_name_colname |
      | sample_table        |
      | subsample_table     |
      | get_sample          |
      | get_samples         |

  Scenario Outline: Complete inside rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule Name:
      <section>:
        pep.
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | <property>|
    Examples:
      | section | property        |
      | section | amendments      |
      | run     | list_amendments |

  Scenario Outline: Complete in injections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
           <section>: "{pep.}"
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | amendments      |
      | list_amendments |
      | config          |
    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | rule       | message |
      | checkpoint | shell   |

  Scenario: Complete in "onstart" section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    onstart :
           pep.
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | amendments      |
      | list_amendments |
      | config          |

  Scenario Outline: No completion in injections for wildcards expanding/defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{pep.}"
        """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | amendments      |
      | list_amendments |
      | config          |

    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | output  |
      | rule       | log     |
      | checkpoint | input   |

  Scenario: Complete in not-empty context
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     pep.c
     """
    When I put the caret after pep.c
    And I invoke autocompletion popup
    Then completion list should contain:
      | config |

  Scenario Outline: Parenthesis inserted after method completion
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      pep.
      """
    When I put the caret after pep.
    Then I invoke autocompletion popup, select "<item>" lookup item and see a text:
      """
      pep.<inserted_text>
      """
    Examples:
      | item        | inserted_text |
      | config      | config        |
      | get_samples | get_samples() |