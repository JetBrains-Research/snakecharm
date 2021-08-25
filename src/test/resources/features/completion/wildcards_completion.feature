Feature: Completion for wildcards

  Scenario Outline: Wildcards are collected from correct sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      conda: "{wildcard1}"
      resources: "{wildcard2}"
      group: "{wildcard3}"
      benchmark: "{wildcard4}"
      log: "{wildcard5}"
      output: "{wildcard6}"
      params: "{wildcard7}"
      envmodules: "{non-wildcard8}"
      input: "{}"
      message: "{non-wildcard1}"
      name: "{non-wildcard9}"
      cwl: "{non-wildcard2}"
      script: "{non-wildcard3}"
      shell: "{non-wildcard4}"


    use rule NAME as NAME2 with:
      input: "use{}"
    """
    When I put the caret after <signature>
    And I invoke autocompletion popup
    Then completion list should contain:
      # wildcards defining:
      | wildcard6 |
      | wildcard5 |
      | wildcard4 |
      # wildcards expanding:
      | wildcard1 |
      | wildcard2 |
      | wildcard3 |
      | wildcard7 |
    And completion list shouldn't contain:
      | non-wildcard1 |
      | non-wildcard2 |
      | non-wildcard3 |
      | non-wildcard4 |
      | non-wildcard8 |
      | non-wildcard9 |
    Examples:
      | rule_like  | signature    |
      | rule       | input: "{    |
      | rule       | input: "use{ |
      | checkpoint | input: "{    |
      | checkpoint | input: "use{ |

  Scenario Outline: Wildcards are collected from all appropriate injections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "{wildcard1}", expand("{wildcard2}"),
          "{wildcard3}" + "{wildcard4}" + "{wildcard5}",
          "{wildcard6}" '{wildcard7}' f"{{wildcard8}}"
          kwd="{wildcard6}"
      input: "{}"

    use rule NAME as NAME2 with:
      input: "use{}"
    """
    When I put the caret after <signature>
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcard1 |
      | wildcard3 |
      | wildcard4 |
      | wildcard5 |
      | wildcard6 |
      | wildcard7 |
      | wildcard8 |
    And completion list shouldn't contain:
      | wildcard2 |

    Examples:
      | rule_like  | signature    |
      | rule       | input: "{    |
      | rule       | input: "use{ |
      | checkpoint | input: "{    |
      | checkpoint | input: "use{ |

  Scenario Outline: Wildcards are collected only from a parent rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> ANOTHER_NAME:
        output: "{wildcard1}"

    <rule_like> NAME:
      output: "{wildcard2}"
      input: "{}"
    """
    When I put the caret after input: "{
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcard2 |
    And completion list shouldn't contain:
      | wildcard1 |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete a wildcard name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "{wildcard}"
      input: "{}"

    use rule NAME as NAME2 with:
      log: "{}"
    """
    When I put the caret after input: "{
    Then I invoke autocompletion popup, select "wildcard" lookup item and see a text:
    """
    <rule_like> NAME:
      output: "{wildcard}"
      input: "{wildcard}"

    use rule NAME as NAME2 with:
      log: "{}"
    """
    When I put the caret after log: "{
    Then I invoke autocompletion popup, select "wildcard" lookup item and see a text:
    """
    <rule_like> NAME:
      output: "{wildcard}"
      input: "{wildcard}"

    use rule NAME as NAME2 with:
      log: "{wildcard}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion list after wildcards keyword
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "{wildcard1}"
      output: "{wildcard2}{wildcard3}"
      shell: "{wildcards<accessor>}"
    """
    When I put the caret after <signature>
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcard2 |
      | wildcard3 |
    Examples:
      | rule_like  | accessor | signature  |
      | rule       | .        | wildcards. |
      | rule       | []       | wildcards[ |
      | checkpoint | .        | wildcards. |

  Scenario Outline: Insertion handler after wildcards.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "{wildcard}"
      shell: "{wildcards.}"
    """
    When I put the caret after wildcards.
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      output: "{wildcard}"
      shell: "{wildcards.wildcard}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Expand injections not in wildcards
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>:
            <text>
        shell: "{wildcards.fo}"
    """
    And I put the caret after wildcards.
    When I invoke autocompletion popup
    Then completion list shouldn't contain:
      | prefix |
    Examples:
      | rule_like  | section | text                                      |
      | rule       | output  | expand("{prefix}", prefix="p")            |
      | rule       | input   | expand("{prefix}", prefix="p")            |
      | rule       | input   | lambda wd: expand("{prefix}", prefix="p") |
      | checkpoint | output  | expand("{prefix}", prefix="p")            |
      | checkpoint | input   | lambda wd: expand("{prefix}", prefix="p") |


  Scenario Outline: Do not collect wildcard from lambdas (manual injection case)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: lambda wildcards: <text>
        shell: "{wildcards.fo}"
    """
    And I put the caret at prefix}"
    And I inject SmkSL at a caret
    Then I expect language injection on "{prefix}"
    And I put the caret after wildcards.
    When I invoke autocompletion popup
    Then completion list shouldn't contain:
      | prefix |
    Examples:
      | rule_like  | section | text                |
      | rule       | input   | "{prefix}"          |
      | rule       | input   | ancient("{prefix}") |
      | rule       | input   | temp("{prefix}")    |
      | rule       | input   | foo("{prefix}")     |
      | checkpoint | input   | "{prefix}"          |
      | checkpoint | input   | ancient("{prefix}") |


  Scenario Outline: Completion in rule run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
          output:  "{o1}/{o2}"
          input:  "{i1}"
          message:  "{non_wc}"
          run:
             wildcards.smth
      """
    And I put the caret after wildcards.
    When I invoke autocompletion popup
    Then completion list should contain:
      | o1 |
      | o2 |
      | i1 |
    And completion list shouldn't contain:
      | non_wc |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: No completion in wildcard references which looks like qualified
    Given a snakemake project
    Given I open a file "foo.smk" with text
       """
       <rule_like> NAME:
           output: "{sample}"
           input: "{wildcards.sample}"
       """
    And I put the caret after wildcards.
    When I invoke autocompletion popup
    And completion list shouldn't contain:
      | sample |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |