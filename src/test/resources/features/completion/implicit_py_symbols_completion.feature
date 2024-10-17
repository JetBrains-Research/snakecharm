Feature: Completion in python part of snakemake file
  Auto-complete runtime magic from snakemake

  Scenario: Complete at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    foo = 1;
    """
    When I put the caret after foo = 1;
    And I invoke autocompletion popup
    Then completion list should contain:
      | expand        |
      | temp          |
      | update        |
      | before_update |
      | directory     |
      | protected     |
      | touch         |
      | unpack        |
      | ancient       |
      | ensure        |
      | shell         |
      | config        |
      | rules         |
      | input         |
      | pep           |

  Scenario: Complete at top-level (GTE 6.1)
    Given a snakemake:6.1 project
    Given I open a file "foo.smk" with text
    """
    foo = 1;
    """
    When I put the caret after foo = 1;
    And I invoke autocompletion popup
    Then completion list should contain:
      | scatter |
      | gather  |
      | dynamic  |

  Scenario: Complete at top-level (<= 7.32.4)
    Given a snakemake:7.32.4 project
    Given I open a file "foo.smk" with text
    """
    foo = 1;
    """
    When I put the caret after foo = 1;
    And I invoke autocompletion popup
    Then completion list should contain:
      | dynamic  |

  Scenario: Not-completed at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    foo = 1;
    """
    When I put the caret after foo = 1;
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
    | output    |
    | params    |
    | wildcards |

  Scenario: Not-completed after dot
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    a = 30
    a.
    """
    When I put the caret after a.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | expand |
      | config |
      | shell  |
      | pep    |

  Scenario: Complete in rules params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input:

    """
    When I put the caret after input:
    And I invoke autocompletion popup
    Then completion list should contain:
      | expand |
      | config |
      | rules  |
      | shell  |
      | pep    |

  Scenario: Complete in rules params section (GTE 6.1)
    Given a snakemake:6.1 project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input:

    """
    When I put the caret after input:
    And I invoke autocompletion popup
    Then completion list should contain:
    | scatter |
    | gather  |

  Scenario: Complete in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
        output: "path/to/output"
        run:
    """
    When I put the caret after run:
    And I invoke autocompletion popup
    Then completion list should contain:
      | expand    |
      | config    |
      | rules     |
      | shell     |
      | input     |
      | output    |
      | params    |
      | wildcards |
      | resources |
      | log       |
      | threads   |
      | version   |
      | rule      |
      | jobid     |
      | pep       |

  Scenario: Complete in run section  (GTE 6.1)
      Given a snakemake:6.1 project
    Given I open a file "foo.smk" with text
    """
    rule all:
        output: "path/to/output"
        run:
    """
    When I put the caret after run:
    And I invoke autocompletion popup
    Then completion list should contain:
      | scatter |
      | gather  |

  Scenario: Not-completed in rule outside run section
     Given a snakemake project
     Given I open a file "foo.smk" with text
     """
      rule NAME:
        log:
          foo = 1
     """
     When I put the caret at foo = 1
     And I invoke autocompletion popup
     Then completion list shouldn't contain:
     | output    |
     | params    |
     | wildcards |
     | wildcards |
     | resources |
     | log       |
     | threads   |
     | version   |
     | rule      |
     | jobid     |

  Scenario Outline: Not-completed in top level python block
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <block>:
         foo = 1
      """
    When I put the caret at foo = 1
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | output    |
      | params    |
      | wildcards |
      | wildcards |
      | resources |
      | log       |
      | threads   |
      | version   |
      | rule      |
      | jobid     |
    Examples:
      | block     |
      | onstart   |
      | onerror   |
      | onsuccess |


  Scenario Outline: Parenthesis inserted after method completion
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      foo = 1;
      """
      When I put the caret after foo = 1;
      Then I invoke autocompletion popup, select "<item>" lookup item and see a text:
      """
      foo = 1;<inserted_text>
      """
    Examples:
      | item   | inserted_text |
      | expand | expand()      |
      | config | config        |
      | shell  | shell()       |
      | pep    | pep           |


  #noinspection SpellCheckingInspection
  Scenario: Complete in not-empty context and select by type text
     Given a snakemake project
     Given I open a file "foo.smk" with text
     """
     expan
     """
     When I put the caret after expan
      # Several 'expand' are in completion
     Then I invoke autocompletion popup, select "expand" lookup item with type text "snakemake.io" and see a text:
     """
     expand()
     """


  Scenario Outline: Completion isn't provided for not snakemake python dialects files
    Given a snakemake project
    Given I open a file "foo.<ext>" with text
    """
    a = 1;
    """
    When I put the caret after a = 1;
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | expand |
      | config |
      | rules  |
      | pep    |

    Examples:
      | ext |
      | py  |
      | pyi |

  Scenario Outline: Completion in injections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{}"
        """
    When I put the caret after "{
    And I invoke autocompletion popup
    Then completion list should contain:
      | rules       |
      | checkpoints |
      | config      |
      | pep         |

    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | rule       | message |
      | checkpoint | shell   |

  Scenario Outline: No completion in injections for wildcards expanding/defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{}"
        """
    When I put the caret after "{
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | rules       |
      | checkpoints |
      | config      |
      | pep         |

    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | output  |
      | rule       | log     |
      | checkpoint | input   |