Feature: Yet-undefined name after rules/checkpoints

  Scenario Outline: Name not defined yet
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      input: <section>s.ANOTHER_NAME

    <section> ANOTHER_NAME:
      input: "in.txt"
    """
    And Undefined name inspection is enabled
    Then I expect inspection error on <ANOTHER_NAME> with message
    """
    This name hasn't been defined yet: ANOTHER_NAME
    """
    When I check highlighting errors
    Examples:
    | section    |
    | rule       |
    | checkpoint |

  Scenario Outline: All names are defined
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      input: "a.txt"

    <section> ANOTHER_NAME:
      input: <section>s.NAME
    """
    And Undefined name inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
    | section    |
    | rule       |
    | checkpoint |

  Scenario Outline: Unresolved name isn't undefined
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> ANOTHER:
      input: <section>s.NAME
    """
    And Undefined name inspection is enabled
    And Unresolved reference inspection is enabled
    Then I expect inspection warning on <NAME> with message
    """
    Cannot find reference 'NAME' in '<section>s'
    """
    When I check highlighting errors
    Examples:
    | section    |
    | rule       |
    | checkpoint |

  Scenario Outline: No inspection in 'run' section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> ANOTHER:
      run: <section>s.NAME
    """
    And Undefined name inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
    | section    |
    | rule       |
    | checkpoint |