package app.cozitopia.ui.checklist

import app.cozitopia.domain.Assignee
import app.cozitopia.domain.AssigneeKind

sealed interface AssigneeOption {
    data object Everyone : AssigneeOption

    data object Unassigned : AssigneeOption

    data class Person(val userId: String) : AssigneeOption
}

fun toggleAssignee(current: Assignee, option: AssigneeOption): Assignee = when (option) {
    AssigneeOption.Everyone -> Assignee.Everyone
    AssigneeOption.Unassigned -> Assignee.Unassigned
    is AssigneeOption.Person -> {
        val selected = if (current.kind == AssigneeKind.PEOPLE) current.userIds else emptySet()
        val updated = if (option.userId in selected) selected - option.userId else selected + option.userId
        if (updated.isEmpty()) Assignee.Unassigned else Assignee(AssigneeKind.PEOPLE, updated)
    }
}

fun isSelected(current: Assignee, option: AssigneeOption): Boolean = when (option) {
    AssigneeOption.Everyone -> current.kind == AssigneeKind.EVERYONE
    AssigneeOption.Unassigned -> current.kind == AssigneeKind.UNASSIGNED
    is AssigneeOption.Person -> current.kind == AssigneeKind.PEOPLE && option.userId in current.userIds
}
