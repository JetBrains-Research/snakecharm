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
    | expand      |
    | temp        |
    | directory   |
    | directory   |
    | protected   |
    | touch       |
    | dynamic     |
    | unpack      |
    | ancient     |
    | shell       |
    | config      |
    | rules       |
    | input       |

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
    | expand    |
    | config    |
    | shell     |

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
    | expand      |
    | config      |
    | rules       |
    | shell       |


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
      | item    | inserted_text |
      | expand  | expand()      |
      | config  | config        |
      | shell   | shell()       |

  #noinspection SpellCheckingInspection
  Scenario: Complete in not-empty context
     Given a snakemake project
     Given I open a file "foo.smk" with text
     """
     expa
     """
     When I put the caret after expa
     Then I invoke autocompletion popup and see a text:
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

    Examples:
      | ext |
      | py  |
      | pyi |
