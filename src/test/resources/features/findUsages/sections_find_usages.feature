Feature: Find Usages for rule sections

  Scenario Outline: Usages for section declaration name
      Given a snakemake project
      Given I open a file "foo.smk" with text
        """
        <rule_like> foo:
          <section>:
              "some",
              l = "{<section>}.txt"
          shell: "{<section>[0]} {<section>.l} {wildcards.<section>}"
          run:
            p1 = <section>[0]
            p2 = <section>.l
        """
      When I put the caret at <section>:
      And I invoke find usages
      Then find usages shows me following references:
        | file    | offset   | length   |
        | foo.smk | <offset1> | <length> |
        | foo.smk | <offset2> | <length> |
        | foo.smk | <offset3> | <length> |
        | foo.smk | <offset4> | <length> |
      Examples:
        | rule_like  | section | offset1 | offset2 | offset3 | offset4 | length |
        | rule       | input   | 68      | 79      | 123     | 141     | 5      |
        | rule       | output  | 70      | 82      | 128     | 147     | 6      |
        | checkpoint | params  | 76      | 88      | 134     | 153     | 6      |
        | checkpoint | log     | 70      | 79      | 119     | 135     | 3      |