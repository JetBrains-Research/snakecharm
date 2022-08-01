Feature: Completion for 'use' and 'module' sections

  Scenario: Complete rule name, declared in use section in local .smk file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule * from MODULE as last_rule

    use rule a,b,c from MODULE as other_*

    use rule NAME as NAME2 with:
      input: "data_file.txt"

    use rule zZzz from MODULE as with:
      input: "log.log"

    rule my_rule:
      log: rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list should contain:
      | last_rule |
      | other_a   |
      | other_b   |
      | other_c   |
      | NAME2     |
      | zZzz      |
    Then completion list shouldn't contain:
      | NAME    |
      | other_* |
      | *       |
      | MODULE  |

  Scenario Outline: Complete rule name, declared in module
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> rule_name:
      log: "log_file.txt"

    use rule rule_name as new_rule_name with:
      threads: 3
    """
    Given I open a file "foo.smk" with text
    """
    module MODULE_2:
      snakefile: "boo.smk"

    use rule * from MODULE_2 as not_*

    rule my_rule:
      log: rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list should contain:
      | not_rule_name     |
      | not_new_rule_name |
    Then completion list shouldn't contain:
      | *             |
      | not_*         |
      | MODULE_2      |
      | rule_name     |
      | new_rule_name |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete rule name using complex patterns
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> rule_name:
      log: "log_file.txt"

    """
    Given I open a file "foo.smk" with text
    """
    module MODULE_2:
      snakefile: "boo.smk"

    use rule * from MODULE_2 as not_*_AND_*_AND_*

    use rule a,b,c from MODULE as other_* XXX * YYY ZZZ

    rule my_rule:
      log: rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list should contain:
      | not_rule_name_AND_rule_name_AND_rule_name |
      | other_aXXXaYYYZZZ                         |
      | other_bXXXbYYYZZZ                         |
      | other_cXXXcYYYZZZ                         |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete rule name, declared in .smk file included to module
    Given a snakemake project
    And a file "boo.smk" with text
    """
    include: "zoo.smk"
    """
    And a file "zoo.smk" with text
    """
    <rule_like> zoo_rule: threads: 1

    use rule zoo_rule as rule_from_zoo with: threads: 2
    """
    Given I open a file "foo.smk" with text
    """
    module MODULE_2:
      snakefile: "boo.smk"

    use rule * from MODULE_2 as not_*

    rule my_rule:
      log: rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list should contain:
      | not_zoo_rule      |
      | not_rule_from_zoo |
    Then completion list shouldn't contain:
      | *             |
      | not_*         |
      | MODULE        |
      | zoo_rule      |
      | rule_from_zoo |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario:  Complete use rule name definition by rule or checkpoint, declared in use section in local .smk file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    use rule * from MODULE as last_rule

    use rule a,b,c from MODULE as other_*

    use rule NAME as NAME2 with:
      input: "data_file.txt"

    use rule zZzz from MODULE as with:
      input: "log.log"

    rule rule_name:
      input: "rule_file"

    checkpoint checkpoint_name:
      input: "checkpoint_file"

    use rule # here
    """
    When I put the caret at # here
    And I invoke autocompletion popup
    Then completion list should contain:
      | last_rule       |
      | other_a         |
      | other_b         |
      | other_c         |
      | NAME2           |
      | zZzz            |
      | rule_name       |
      | checkpoint_name |

  Scenario Outline:  Complete use rule name definition by rule or checkpoint, declared in module
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> rule_name:
      log: "log_file.txt"

    use rule rule_name as new_rule_name with:
      threads: 3
    """
    Given I open a file "foo.smk" with text
    """
    module MODULE_2:
      snakefile: "boo.smk"

    use rule * from MODULE_2 as not_*

    use rule # here
    """
    When I put the caret at # here
    And I invoke autocompletion popup
    Then completion list should contain:
      | not_rule_name     |
      | not_new_rule_name |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete use rule name definition by rule or checkpoint, declared in .smk file included to module
    Given a snakemake project
    And a file "boo.smk" with text
    """
    include: "zoo.smk"
    """
    And a file "zoo.smk" with text
    """
    <rule_like> zoo_rule: threads: 1

    use rule zoo_rule as rule_from_zoo with: threads: 2
    """
    Given I open a file "foo.smk" with text
    """
    module MODULE_2:
      snakefile: "boo.smk"

    use rule * from MODULE_2 as not_*

    use rule # here
    """
    When I put the caret at # here
    And I invoke autocompletion popup
    Then completion list should contain:
      | not_zoo_rule      |
      | not_rule_from_zoo |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario: No StackOverflowError with cyclic imports
    Given a snakemake project
    And a file "boo.smk" with text
    """
    include: "zoo.smk"

    rule boo_rule:
      log: "log_file1.txt"
    """
    And a file "zoo.smk" with text
    """
    include: "boo.smk"

    rule zoo_rule:
      log: "log_file2.txt"
    """
    Given I open a file "foo.smk" with text
    """
    module m:
      snakefile: "boo.smk"

    use rule * from m as other_*

    rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list should contain:
      | other_boo_rule |
      | other_zoo_rule |

  Scenario: No StackOverflowError with cyclic module imports
    Given a snakemake project
    And a file "boo.smk" with text
    """
    module m:
      snakefile: "zoo.smk"

    use rule * from m
    """
    And a file "zoo.smk" with text
    """
    module m:
      snakefile: "boo.smk"

    use rule * from m
    """
    Given I open a file "foo.smk" with text
    """
    module m:
      snakefile: "boo.smk"

    use rule * from m as other_*

    rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | m |

