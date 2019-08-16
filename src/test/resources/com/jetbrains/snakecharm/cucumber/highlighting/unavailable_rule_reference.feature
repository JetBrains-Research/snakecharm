Feature: unavailable rule references inspection

  Scenario Outline: rule from a file included in the current file
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule boo: input: "file.txt"
    """
    Given I open a file "foo.smk" with text
    """
    rule foo:
       input:
          "file.txt"
    include: "boo.smk"
    <section>: foo <separator> boo
    """
    And Unavailable Rule References inspection is enabled
    Then I expect no inspection weak warning
    When I check highlighting weak warnings
    Examples:
      | section    | separator |
      | localrules | ,         |
      | ruleorder  | >         |

  Scenario Outline: rule from a file not included in the current file
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule boo: input: "file.txt"
    """
    Given I open a file "foo.smk" with text
    """
    rule foo:
       input:
          "file.txt"
    <section>: foo <separator> boo
    """
    And Unavailable Rule References inspection is enabled
    Then I expect inspection weak warning on <boo> in <foo <separator> boo> with message
    """
    This rule is not included/present in this file.
    """
    When I check highlighting weak warnings
    Examples:
      | section    | separator |
      | localrules | ,         |
      | ruleorder  | >         |

  Scenario Outline: rule with the same name is present in an included file
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule foo: input: "file.txt"
    """
    Given I open a file "foo.smk" with text
    """
    rule foo:
       input:
          "file.txt"
    include: "boo.smk"
    <section>: foo #
    """
    And Unavailable Rule References inspection is enabled
    Then I expect inspection error on <foo> in <foo #> with message
    """
    This rule name is already used by another rule.
    """
    When I check highlighting weak warnings
    Examples:
      | section    |
      | localrules |
      | ruleorder  |

  Scenario Outline: rule with the same name is present in an indexed but not included file
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule foo: input: "file.txt"
    """
    Given I open a file "foo.smk" with text
    """
    rule foo:
       input:
          "file.txt"
    <section>: foo #
    """
    And Unavailable Rule References inspection is enabled
    Then I expect no inspection error
    When I check highlighting weak warnings
    Examples:
      | section    |
      | localrules |
      | ruleorder  |
