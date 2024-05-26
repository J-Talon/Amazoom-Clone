package com.amazoom;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

import java.sql.SQLException;
import java.util.List;

abstract class Editable<T> extends Updatable {
    abstract String editor(T id, Model model, HttpSession session);

    abstract String edit(T id, List<String> keys, List<String> newValues, Model model, HttpSession session);

    abstract String delete(T id, Model model, HttpSession session);

    static String delete(Class<? extends Editable<?>> editable, Object id, Model model, HttpSession session) {
        try {
            Editable.dao(editable).deleteById(id);
        } catch (SQLException e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }

        model.addAttribute("message", "Deleted successfully");
        return "success";
    }
}

record EditInfo(String field, String value, boolean modifiable) {}