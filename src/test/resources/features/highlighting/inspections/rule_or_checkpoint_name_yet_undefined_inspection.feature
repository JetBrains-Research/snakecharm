Feature: Inspection: Yet-undefined name after rules/checkpoints

  Scenario Outline: Name not defined yet
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      input: <section>s.ANOTHER_NAME

    <section> ANOTHER_NAME:
      input: "in.txt"
    """
    And Rule or Checkpoint Name yet undefined inspection is enabled
    Then I expect inspection error on <ANOTHER_NAME> in <s.ANOTHER_NAME> with message
    """
    Rule or Checkpoint name 'ANOTHER_NAME' has not been defined yet
    """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |


  Scenario Outline: Same name as container
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <section> NAME:
       input: <section>s.NAME
     """
    And Rule or Checkpoint Name yet undefined inspection is enabled
    Then I expect inspection error on <NAME> in <s.NAME> with message
     """
     Rule or Checkpoint name 'NAME' has not been defined yet
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
    And Rule or Checkpoint Name yet undefined inspection is enabled
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
    And Rule or Checkpoint Name yet undefined inspection is enabled
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection error on <NAME> with message
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
    <section> NAME:
      input: "foo"

    <section> ANOTHER:
      run: <section>s.<name>
    """
    And Rule or Checkpoint Name yet undefined inspection is enabled
    Then I expect no inspection error
    When I check highlighting errors
    Examples:
      | section    | name    |
      | rule       | ANOTHER |
      | checkpoint | ANOTHER |
      | rule       | NAME    |
      | checkpoint | NAME    |