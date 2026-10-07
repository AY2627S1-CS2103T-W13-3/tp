package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.commands.SetFollowUpCommand.MESSAGE_USAGE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FOLLOW_UP_DATE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FOLLOW_UP_DESCRIPTION;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.ClearFollowUpCommand;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.SetFollowUpCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses the record, replace, and clear forms of the follow-up command. */
public final class FollowUpCommandParser implements Parser<Command> {
    public static final String COMMAND_WORD = "followup";

    @Override
    public Command parse(String args) throws ParseException {
        ArgumentMultimap arguments = ArgumentTokenizer.tokenize(args, PREFIX_FOLLOW_UP_DATE,
                PREFIX_FOLLOW_UP_DESCRIPTION);
        String preamble = arguments.getPreamble();
        boolean hasDate = arguments.getValue(PREFIX_FOLLOW_UP_DATE).isPresent();
        boolean hasDescription = arguments.getValue(PREFIX_FOLLOW_UP_DESCRIPTION).isPresent();

        if (!hasDate && !hasDescription && isClearForm(preamble)) {
            return new ClearFollowUpCommand(FollowUpParserUtil.parseIndex(preamble.split(" +")[0]));
        }
        if (!hasDate || !hasDescription || !preamble.matches("[^ ]+")) {
            throw invalidFormat();
        }

        arguments.verifyNoDuplicatePrefixesFor(PREFIX_FOLLOW_UP_DATE, PREFIX_FOLLOW_UP_DESCRIPTION);
        Index targetIndex = FollowUpParserUtil.parseIndex(preamble);
        return new SetFollowUpCommand(targetIndex, arguments.getValue(PREFIX_FOLLOW_UP_DATE).orElseThrow(),
                arguments.getValue(PREFIX_FOLLOW_UP_DESCRIPTION).orElseThrow());
    }

    private static boolean isClearForm(String preamble) {
        return preamble.matches("[^ ]+ +clear");
    }

    private static ParseException invalidFormat() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, MESSAGE_USAGE));
    }
}
