Feature: Rename Sections

  Scenario Outline: Rename is forbidden
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
        <section>: "foo"
        message: "{<section>}"
        run:
          print(<section>)
      """
    When I put the caret at <section>: "foo"
    When I invoke rename with name "doo" and get error "Section keyword rename not allowed."
    Examples:
      | rule_like  | section |
      | rule       | output  |
      | rule       | input   |
      | checkpoint | input   |