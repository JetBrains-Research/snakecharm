Feature: Snakemake specific Inspections Suppress Actions

  Scenario Outline: Suppress for section argument, rule-like quick fixes
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>:
            <content>
    """
    When I put the caret at <signature>
    Then should see "<quick_fix>" quick fix for highlighted context:
    """
    <context>
    """
    And Inspection "FAKE_SMK_QUICKFIX" shouldn't be suppressed
    Examples:
      | quick_fix                     | rule_like   | section | content                     | signature | context                                              |
      | Suppress for section argument | rule        | input   | "something"                 | thing"    | "something"                                          |
      | Suppress for section argument | rule        | input   | f"foo{bb}""something"       | thing"    | f"foo{bb}""something"                                |
      | Suppress for section argument | rule        | input   | "foo"\n        "something"  | thing"    | "foo"\n        "something"                           |
      | Suppress for section argument | rule        | input   | "foo",\n        "something" | thing"    | "something"                                          |
      | Suppress for section argument | rule        | input   | fffffa                      | fffffa    | fffffa                                               |
      | Suppress for section argument | checkpoint  | output  | "something"                 | thing"    | "something"                                          |
      | Suppress for section          | rule        | input   | fffffa                      | fffa      | input:\n        fffffa                               |
      | Suppress for section          | rule        | run     | fffffa                      | fffa      | run:\n        fffffa                                 |
      | Suppress for section          | checkpoint  | run     | fffffa                      | fffa      | run:\n        fffffa                                 |
      | Suppress for rule/subworkflow | rule        | input   | "something"                 | thing"    | rule NAME:\n    input:\n        "something"          |
      | Suppress for rule/subworkflow | rule        | output  | "something"                 | tput      | rule NAME:\n    output:\n        "something"         |
      | Suppress for rule/subworkflow | checkpoint  | input   | "something"                 | thing"    | checkpoint NAME:\n    input:\n        "something"    |
      | Suppress for rule/subworkflow | checkpoint  | output  | "something"                 | tput      | checkpoint NAME:\n    output:\n        "something"   |
      | Suppress for rule/subworkflow | subworkflow | workdir | "something"                 | thing"    | subworkflow NAME:\n    workdir:\n        "something" |

  Scenario Outline: Suppress for section argument quick fix with suppress comment
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>:
            # noinspection FAKE_SMK_QUICKFIX
            <content>
    """
    When I put the caret at <signature>
    Then should see "Suppress for section argument" quick fix for highlighted context:
    """
    """
    And Inspection "FAKE_SMK_QUICKFIX" should be suppressed
    Examples:
      | rule_like  | section | content                    | signature |
      | rule       | input   | "something"                | thing"    |
      | rule       | input   | f"foo{bb}""something"      | thing"    |
      | rule       | input   | "foo"\n        "something" | thing"    |
      | rule       | input   | fffffa                     | fffffa    |
      | checkpoint | output  | "something"                | thing"    |

  Scenario Outline: Suppress for section quick fix with suppress comment
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        # noinspection FAKE_SMK_QUICKFIX
        <section>:
            <content>
    """
    When I put the caret at <signature>
    Then should see "Suppress for section" quick fix for highlighted context:
    """
    """
    And Inspection "FAKE_SMK_QUICKFIX" should be suppressed
    Examples:
      | rule_like  | section | content     | signature |
      | rule       | input   | "something" | thing"    |
      | rule       | run     | ffffffa     | fffa      |
      | checkpoint | run     | "something" | thing"    |

  Scenario Outline: Suppress for section argument quick fix with suppress comment (py call element)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>:
            # noinspection FAKE_SMK_QUICKFIX
            key=some_fun(value)
    """
    When I put the caret at lue)
    Then should see "Suppress for section argument" quick fix for highlighted context:
    """
    """
    And Inspection "FAKE_SMK_QUICKFIX" should be suppressed
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | checkpoint | output  |

  Scenario Outline: Suppress for section rule-like quick fix with suppress comment
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    # noinspection FAKE_SMK_QUICKFIX
    <rule_like> NAME:
        <section>:
            "something"
    """
    When I put the caret at <signature>
    Then should see "<quick_fix>" quick fix for highlighted context:
    """
    """
    And Inspection "FAKE_SMK_QUICKFIX" should be suppressed
    Examples:
      | quick_fix                     | rule_like   | section | signature |
      | Suppress for rule/subworkflow | rule        | output  | thing"    |
      | Suppress for rule/subworkflow | rule        | output  | tput      |
      | Suppress for rule/subworkflow | checkpoint  | output  | thing"    |
      | Suppress for rule/subworkflow | checkpoint  | output  | tput      |
      | Suppress for rule/subworkflow | subworkflow | workdir | thing"    |
      | Suppress for rule/subworkflow | subworkflow | workdir | orkdir    |

  Scenario Outline: No suppress section args outside section params
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
        # comment
        <section>: "file"
    """
    When I put the caret at <signature>
    Then shouldn't see "Suppress for section argument" quick fix
    Examples:
      | rule_like  | section | signature |
      | rule       | input   | nput:     |
      | rule       | input   | : "file"  |
      | rule       | input   | comment   |
      | checkpoint | input   | nput:     |

  Scenario Outline: No suppress section outside section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
        # comment
        <section>: "file"
    """
    When I put the caret at comment
    Then shouldn't see "Suppress for section" quick fix
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | checkpoint | input   |

  Scenario Outline: No suppress out of section argument
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <content>
    """
    When I put the caret at fffffa
    Then shouldn't see "Suppress for section argument" quick fix
    Then shouldn't see "Suppress for section" quick fix
    Then shouldn't see "Suppress for rule/subworkflow" quick fix
    Then shouldn't see "Suppress for rule/subworkflow" quick fix
    Then shouldn't see "Suppress for workflow" quick fix
    Examples:
      | content                                 |
      | if True:\n    ffffffffa                 |
      | boo(ffffffffa)                          |
      | rule foo:\n    run:\n    ffffffffa      |
      | rule foo:\n    run:\n    boo(ffffffffa) |
