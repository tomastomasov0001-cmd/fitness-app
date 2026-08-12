#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fitness App - Editor cviků
Windows aplikace pro vytváření a správu cviků pro Fitness App.
"""

import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import json
import os
from datetime import datetime
from typing import List, Dict, Optional

# Cesta k uloženým cvikům (ve stejné složce jako exe/script)
def get_data_path():
    """Vrátí cestu k souboru s uloženými cviky."""
    if getattr(sys, 'frozen', False):
        # Spuštěno jako EXE
        base_path = os.path.dirname(sys.executable)
    else:
        # Spuštěno jako Python script
        base_path = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(base_path, "exercises_data.json")

import sys

class Exercise:
    """Třída reprezentující cvik."""

    def __init__(self, name: str = "", description: str = "",
                 has_sets: bool = True, has_reps: bool = True,
                 has_weight: bool = True, has_time: bool = False,
                 default_sets: int = 3):
        self.id = 0  # ID se přiřadí při exportu
        self.name = name
        self.description = description
        self.has_sets = has_sets
        self.has_reps = has_reps
        self.has_weight = has_weight
        self.has_time = has_time
        self.default_sets = default_sets

    def to_dict(self) -> Dict:
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "hasSets": self.has_sets,
            "hasReps": self.has_reps,
            "hasWeight": self.has_weight,
            "hasTime": self.has_time,
            "defaultSets": self.default_sets
        }

    @staticmethod
    def from_dict(data: Dict) -> 'Exercise':
        return Exercise(
            name=data.get("name", ""),
            description=data.get("description", ""),
            has_sets=data.get("hasSets", True),
            has_reps=data.get("hasReps", True),
            has_weight=data.get("hasWeight", True),
            has_time=data.get("hasTime", False),
            default_sets=data.get("defaultSets", 3)
        )


class ExerciseCreatorApp:
    """Hlavní aplikace pro vytváření cviků."""

    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title("Fitness App - Editor cviků")
        self.root.geometry("950x650")
        self.root.minsize(850, 550)

        # Seznam cviků
        self.exercises: List[Exercise] = []
        self.selected_index: Optional[int] = None
        self.check_vars: List[tk.BooleanVar] = []  # Pro zaškrtávací políčka

        # Nastavení stylu
        self.setup_styles()

        # Vytvoření UI
        self.create_ui()

        # Načtení uložených cviků
        self.load_saved_exercises()

        # Bind klávesové zkratky
        self.root.bind("<Control-s>", lambda e: self.export_exercises())
        self.root.bind("<Control-o>", lambda e: self.import_exercises())
        self.root.bind("<Control-n>", lambda e: self.clear_form())
        self.root.bind("<Delete>", lambda e: self.delete_exercise())
        self.root.bind("<Control-a>", lambda e: self.select_all())

        # Uložení při zavření
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)

    def setup_styles(self):
        """Nastavení stylů pro ttk widgety."""
        style = ttk.Style()
        style.theme_use('clam')

        # Hlavní barvy
        style.configure("TFrame", background="#f5f5f5")
        style.configure("TLabel", background="#f5f5f5", font=("Segoe UI", 10))
        style.configure("TButton", font=("Segoe UI", 10), padding=5)
        style.configure("Header.TLabel", font=("Segoe UI", 14, "bold"))
        style.configure("TCheckbutton", background="#f5f5f5", font=("Segoe UI", 10))

        # Zvýraznění tlačítek
        style.configure("Accent.TButton", font=("Segoe UI", 10, "bold"))
        style.map("Accent.TButton",
                  background=[("active", "#6200EE"), ("!active", "#7C4DFF")])

    def create_ui(self):
        """Vytvoření uživatelského rozhraní."""
        # Hlavní kontejner
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.pack(fill=tk.BOTH, expand=True)

        # Levý panel - formulář
        left_frame = ttk.Frame(main_frame, padding="10")
        left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Pravý panel - seznam
        right_frame = ttk.Frame(main_frame, padding="10")
        right_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        # === LEVÝ PANEL - FORMULÁŘ ===
        ttk.Label(left_frame, text="Nový cvik", style="Header.TLabel").pack(anchor=tk.W, pady=(0, 10))

        # Název
        ttk.Label(left_frame, text="Název cviku:").pack(anchor=tk.W)
        self.name_entry = ttk.Entry(left_frame, width=40, font=("Segoe UI", 11))
        self.name_entry.pack(fill=tk.X, pady=(0, 10))

        # Popis
        ttk.Label(left_frame, text="Popis (volitelné):").pack(anchor=tk.W)
        self.desc_text = tk.Text(left_frame, height=4, width=40, font=("Segoe UI", 10))
        self.desc_text.pack(fill=tk.X, pady=(0, 10))

        # Typ záznamu
        ttk.Label(left_frame, text="Typ záznamu:").pack(anchor=tk.W, pady=(10, 5))

        self.has_sets_var = tk.BooleanVar(value=True)
        self.has_reps_var = tk.BooleanVar(value=True)
        self.has_weight_var = tk.BooleanVar(value=True)
        self.has_time_var = tk.BooleanVar(value=False)

        ttk.Checkbutton(left_frame, text="Série (více opakování cviku)",
                        variable=self.has_sets_var,
                        command=self.toggle_sets).pack(anchor=tk.W)
        ttk.Checkbutton(left_frame, text="Opakování (např. dřepy, kliky)",
                        variable=self.has_reps_var).pack(anchor=tk.W)
        ttk.Checkbutton(left_frame, text="Váha (např. bench press)",
                        variable=self.has_weight_var).pack(anchor=tk.W)
        ttk.Checkbutton(left_frame, text="Čas (např. běh, plank)",
                        variable=self.has_time_var).pack(anchor=tk.W)

        # Výchozí počet sérií
        sets_frame = ttk.Frame(left_frame)
        sets_frame.pack(fill=tk.X, pady=(10, 0))

        ttk.Label(sets_frame, text="Výchozí počet sérií:").pack(side=tk.LEFT)
        self.sets_spinbox = ttk.Spinbox(sets_frame, from_=1, to=20, width=5, font=("Segoe UI", 10))
        self.sets_spinbox.set(3)
        self.sets_spinbox.pack(side=tk.LEFT, padx=(10, 0))

        # Tlačítka
        btn_frame = ttk.Frame(left_frame)
        btn_frame.pack(fill=tk.X, pady=(20, 0))

        self.add_btn = ttk.Button(btn_frame, text="Přidat cvik", command=self.add_exercise, style="Accent.TButton")
        self.add_btn.pack(side=tk.LEFT, padx=(0, 5))

        ttk.Button(btn_frame, text="Vyčistit", command=self.clear_form).pack(side=tk.LEFT)

        # === PRAVÝ PANEL - SEZNAM ===
        ttk.Label(right_frame, text="Seznam cviků", style="Header.TLabel").pack(anchor=tk.W, pady=(0, 10))

        # Tlačítka pro výběr
        select_frame = ttk.Frame(right_frame)
        select_frame.pack(fill=tk.X, pady=(0, 5))

        ttk.Button(select_frame, text="Vybrat vše", command=self.select_all).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(select_frame, text="Zrušit výběr", command=self.deselect_all).pack(side=tk.LEFT)

        # Seznam s posuvníkem a checkboxy
        list_container = ttk.Frame(right_frame)
        list_container.pack(fill=tk.BOTH, expand=True)

        # Canvas pro scrollování
        self.canvas = tk.Canvas(list_container, bg="white", highlightthickness=1, highlightbackground="#ccc")
        scrollbar = ttk.Scrollbar(list_container, orient="vertical", command=self.canvas.yview)

        self.scrollable_frame = ttk.Frame(self.canvas)
        self.scrollable_frame.bind(
            "<Configure>",
            lambda e: self.canvas.configure(scrollregion=self.canvas.bbox("all"))
        )

        self.canvas.create_window((0, 0), window=self.scrollable_frame, anchor="nw")
        self.canvas.configure(yscrollcommand=scrollbar.set)

        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        # Bind mouse wheel
        self.canvas.bind_all("<MouseWheel>", self._on_mousewheel)

        # Počet cviků
        self.count_label = ttk.Label(right_frame, text="Celkem: 0 cviků | Vybráno: 0")
        self.count_label.pack(anchor=tk.W, pady=(5, 0))

        # Tlačítka pro seznam
        list_btn_frame = ttk.Frame(right_frame)
        list_btn_frame.pack(fill=tk.X, pady=(10, 0))

        ttk.Button(list_btn_frame, text="Upravit", command=self.edit_selected).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(list_btn_frame, text="Smazat vybrané", command=self.delete_selected).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(list_btn_frame, text="Smazat vše", command=self.clear_all).pack(side=tk.LEFT)

        # === SPODNÍ LIŠTA ===
        bottom_frame = ttk.Frame(self.root, padding="10")
        bottom_frame.pack(fill=tk.X, side=tk.BOTTOM)

        ttk.Button(bottom_frame, text="Import (Ctrl+O)", command=self.import_exercises).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(bottom_frame, text="Export vybraných (Ctrl+S)", command=self.export_exercises, style="Accent.TButton").pack(side=tk.LEFT)

        ttk.Label(bottom_frame, text="Cviky se automaticky ukládají",
                  foreground="gray").pack(side=tk.RIGHT)

    def _on_mousewheel(self, event):
        """Scrollování kolečkem myši."""
        self.canvas.yview_scroll(int(-1*(event.delta/120)), "units")

    def toggle_sets(self):
        """Přepnutí dostupnosti spinboxu pro série."""
        if self.has_sets_var.get():
            self.sets_spinbox.config(state="normal")
        else:
            self.sets_spinbox.config(state="disabled")

    def clear_form(self):
        """Vyčištění formuláře."""
        self.name_entry.delete(0, tk.END)
        self.desc_text.delete("1.0", tk.END)
        self.has_sets_var.set(True)
        self.has_reps_var.set(True)
        self.has_weight_var.set(True)
        self.has_time_var.set(False)
        self.sets_spinbox.set(3)
        self.sets_spinbox.config(state="normal")
        self.selected_index = None
        self.add_btn.config(text="Přidat cvik")
        self.name_entry.focus()

    def refresh_list(self):
        """Obnovení seznamu cviků."""
        # Smazat všechny widgety
        for widget in self.scrollable_frame.winfo_children():
            widget.destroy()

        self.check_vars.clear()

        # Vytvořit nové položky
        for i, exercise in enumerate(self.exercises):
            self.create_exercise_row(i, exercise)

        self.update_count()

    def create_exercise_row(self, index: int, exercise: Exercise):
        """Vytvoření řádku pro cvik."""
        row_frame = ttk.Frame(self.scrollable_frame)
        row_frame.pack(fill=tk.X, padx=5, pady=2)

        # Checkbox
        var = tk.BooleanVar(value=False)
        self.check_vars.append(var)

        cb = ttk.Checkbutton(row_frame, variable=var, command=self.update_count)
        cb.pack(side=tk.LEFT)

        # Název a typ
        types = []
        if exercise.has_reps:
            types.append("opak.")
        if exercise.has_weight:
            types.append("váha")
        if exercise.has_time:
            types.append("čas")
        type_str = ", ".join(types) if types else "bez záznamu"

        label_text = f"{exercise.name}  [{type_str}]"
        label = ttk.Label(row_frame, text=label_text, font=("Segoe UI", 10))
        label.pack(side=tk.LEFT, padx=(5, 0))

        # Bind double-click pro editaci
        label.bind("<Double-1>", lambda e, idx=index: self.edit_exercise(idx))
        row_frame.bind("<Double-1>", lambda e, idx=index: self.edit_exercise(idx))

    def add_exercise(self):
        """Přidání nebo úprava cviku."""
        name = self.name_entry.get().strip()

        if not name:
            messagebox.showwarning("Chyba", "Zadejte název cviku!")
            self.name_entry.focus()
            return

        exercise = Exercise(
            name=name,
            description=self.desc_text.get("1.0", tk.END).strip(),
            has_sets=self.has_sets_var.get(),
            has_reps=self.has_reps_var.get(),
            has_weight=self.has_weight_var.get(),
            has_time=self.has_time_var.get(),
            default_sets=int(self.sets_spinbox.get())
        )

        if self.selected_index is not None:
            # Úprava existujícího
            self.exercises[self.selected_index] = exercise
        else:
            # Přidání nového
            self.exercises.append(exercise)

        self.refresh_list()
        self.clear_form()
        self.save_exercises()

    def edit_exercise(self, index: int):
        """Úprava cviku podle indexu."""
        self.selected_index = index
        exercise = self.exercises[index]

        # Naplnění formuláře
        self.name_entry.delete(0, tk.END)
        self.name_entry.insert(0, exercise.name)

        self.desc_text.delete("1.0", tk.END)
        self.desc_text.insert("1.0", exercise.description)

        self.has_sets_var.set(exercise.has_sets)
        self.has_reps_var.set(exercise.has_reps)
        self.has_weight_var.set(exercise.has_weight)
        self.has_time_var.set(exercise.has_time)
        self.sets_spinbox.set(exercise.default_sets)

        self.toggle_sets()
        self.add_btn.config(text="Uložit změny")
        self.name_entry.focus()

    def edit_selected(self):
        """Úprava prvního vybraného cviku."""
        for i, var in enumerate(self.check_vars):
            if var.get():
                self.edit_exercise(i)
                return
        messagebox.showinfo("Info", "Vyberte cvik k úpravě.")

    def delete_selected(self):
        """Smazání vybraných cviků."""
        selected_indices = [i for i, var in enumerate(self.check_vars) if var.get()]

        if not selected_indices:
            messagebox.showinfo("Info", "Vyberte cviky ke smazání.")
            return

        count = len(selected_indices)
        msg = f"Opravdu smazat {count} {'cvik' if count == 1 else 'cviků'}?"

        if messagebox.askyesno("Potvrdit smazání", msg):
            # Smazat od konce, aby se nezměnily indexy
            for i in reversed(selected_indices):
                del self.exercises[i]

            self.refresh_list()
            self.clear_form()
            self.save_exercises()

    def clear_all(self):
        """Smazání všech cviků."""
        if not self.exercises:
            return

        if messagebox.askyesno("Potvrdit smazání", "Opravdu smazat všechny cviky?"):
            self.exercises.clear()
            self.refresh_list()
            self.clear_form()
            self.save_exercises()

    def select_all(self):
        """Vybrat všechny cviky."""
        for var in self.check_vars:
            var.set(True)
        self.update_count()

    def deselect_all(self):
        """Zrušit výběr všech cviků."""
        for var in self.check_vars:
            var.set(False)
        self.update_count()

    def update_count(self):
        """Aktualizace počtu cviků."""
        total = len(self.exercises)
        selected = sum(1 for var in self.check_vars if var.get())

        if total == 0:
            text = "Celkem: 0 cviků | Vybráno: 0"
        elif total == 1:
            text = f"Celkem: 1 cvik | Vybráno: {selected}"
        elif total < 5:
            text = f"Celkem: {total} cviky | Vybráno: {selected}"
        else:
            text = f"Celkem: {total} cviků | Vybráno: {selected}"

        self.count_label.config(text=text)

    def export_exercises(self):
        """Export vybraných cviků do JSON souboru."""
        selected_indices = [i for i, var in enumerate(self.check_vars) if var.get()]

        if not selected_indices:
            # Pokud nic není vybráno, zeptej se
            if not self.exercises:
                messagebox.showwarning("Chyba", "Nejsou žádné cviky k exportu!")
                return

            if messagebox.askyesno("Export", "Nejsou vybrány žádné cviky.\nExportovat všechny?"):
                selected_indices = list(range(len(self.exercises)))
            else:
                return

        exercises_to_export = [self.exercises[i] for i in selected_indices]

        filename = filedialog.asksaveasfilename(
            defaultextension=".json",
            filetypes=[("JSON soubory", "*.json"), ("Všechny soubory", "*.*")],
            initialfile="exercises_backup.json",
            title="Exportovat cviky"
        )

        if not filename:
            return

        # Přiřazení ID
        for i, exercise in enumerate(exercises_to_export, start=1):
            exercise.id = i

        backup_data = {
            "version": 1,
            "exportDate": int(datetime.now().timestamp() * 1000),
            "exercises": [ex.to_dict() for ex in exercises_to_export]
        }

        try:
            with open(filename, "w", encoding="utf-8") as f:
                json.dump(backup_data, f, ensure_ascii=False, indent=2)

            messagebox.showinfo("Hotovo", f"Exportováno {len(exercises_to_export)} cviků do:\n{filename}")
        except Exception as e:
            messagebox.showerror("Chyba", f"Nepodařilo se uložit soubor:\n{str(e)}")

    def import_exercises(self):
        """Import cviků z JSON souboru."""
        filename = filedialog.askopenfilename(
            filetypes=[("JSON soubory", "*.json"), ("Všechny soubory", "*.*")],
            title="Importovat cviky"
        )

        if not filename:
            return

        try:
            with open(filename, "r", encoding="utf-8") as f:
                data = json.load(f)

            if "exercises" not in data:
                messagebox.showerror("Chyba", "Neplatný formát souboru!")
                return

            imported = 0
            existing_names = {ex.name.lower() for ex in self.exercises}

            for ex_data in data["exercises"]:
                exercise = Exercise.from_dict(ex_data)

                # Kontrola duplicit
                if exercise.name.lower() not in existing_names:
                    self.exercises.append(exercise)
                    existing_names.add(exercise.name.lower())
                    imported += 1

            self.refresh_list()
            self.save_exercises()

            skipped = len(data["exercises"]) - imported
            if skipped > 0:
                messagebox.showinfo("Hotovo", f"Importováno {imported} cviků.\nPřeskočeno {skipped} duplicit.")
            else:
                messagebox.showinfo("Hotovo", f"Importováno {imported} cviků.")

        except json.JSONDecodeError:
            messagebox.showerror("Chyba", "Neplatný JSON soubor!")
        except Exception as e:
            messagebox.showerror("Chyba", f"Nepodařilo se načíst soubor:\n{str(e)}")

    def save_exercises(self):
        """Uložení cviků do lokálního souboru."""
        data = {
            "version": 1,
            "exercises": [ex.to_dict() for ex in self.exercises]
        }

        try:
            with open(get_data_path(), "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"Chyba při ukládání: {e}")

    def load_saved_exercises(self):
        """Načtení uložených cviků."""
        data_path = get_data_path()

        if not os.path.exists(data_path):
            return

        try:
            with open(data_path, "r", encoding="utf-8") as f:
                data = json.load(f)

            if "exercises" in data:
                for ex_data in data["exercises"]:
                    self.exercises.append(Exercise.from_dict(ex_data))

                self.refresh_list()

        except Exception as e:
            print(f"Chyba při načítání: {e}")

    def on_closing(self):
        """Při zavření okna."""
        self.save_exercises()
        self.root.destroy()


def main():
    root = tk.Tk()

    # Nastavení ikony (pokud existuje)
    try:
        root.iconbitmap("icon.ico")
    except:
        pass

    app = ExerciseCreatorApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
