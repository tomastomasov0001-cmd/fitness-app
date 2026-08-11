#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fitness App - Editor cviků
Windows aplikace pro vytváření a správu cviků pro Fitness App.
"""

import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import json
from datetime import datetime
from typing import List, Dict, Optional

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
        self.root.geometry("900x600")
        self.root.minsize(800, 500)

        # Seznam cviků
        self.exercises: List[Exercise] = []
        self.selected_index: Optional[int] = None

        # Nastavení stylu
        self.setup_styles()

        # Vytvoření UI
        self.create_ui()

        # Bind klávesové zkratky
        self.root.bind("<Control-s>", lambda e: self.export_exercises())
        self.root.bind("<Control-o>", lambda e: self.import_exercises())
        self.root.bind("<Control-n>", lambda e: self.clear_form())
        self.root.bind("<Delete>", lambda e: self.delete_exercise())

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

        # Seznam s posuvníkem
        list_frame = ttk.Frame(right_frame)
        list_frame.pack(fill=tk.BOTH, expand=True)

        scrollbar = ttk.Scrollbar(list_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        self.exercise_listbox = tk.Listbox(list_frame, font=("Segoe UI", 11),
                                           yscrollcommand=scrollbar.set,
                                           selectmode=tk.SINGLE)
        self.exercise_listbox.pack(fill=tk.BOTH, expand=True)
        scrollbar.config(command=self.exercise_listbox.yview)

        self.exercise_listbox.bind("<<ListboxSelect>>", self.on_select)
        self.exercise_listbox.bind("<Double-1>", self.edit_exercise)

        # Počet cviků
        self.count_label = ttk.Label(right_frame, text="Celkem: 0 cviků")
        self.count_label.pack(anchor=tk.W, pady=(5, 0))

        # Tlačítka pro seznam
        list_btn_frame = ttk.Frame(right_frame)
        list_btn_frame.pack(fill=tk.X, pady=(10, 0))

        ttk.Button(list_btn_frame, text="Upravit", command=self.edit_exercise).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(list_btn_frame, text="Smazat", command=self.delete_exercise).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(list_btn_frame, text="Smazat vše", command=self.clear_all).pack(side=tk.LEFT)

        # === SPODNÍ LIŠTA ===
        bottom_frame = ttk.Frame(self.root, padding="10")
        bottom_frame.pack(fill=tk.X, side=tk.BOTTOM)

        ttk.Button(bottom_frame, text="Import (Ctrl+O)", command=self.import_exercises).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(bottom_frame, text="Export (Ctrl+S)", command=self.export_exercises, style="Accent.TButton").pack(side=tk.LEFT)

        ttk.Label(bottom_frame, text="Fitness App - Editor cviků v1.0",
                  foreground="gray").pack(side=tk.RIGHT)

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
        self.exercise_listbox.selection_clear(0, tk.END)
        self.name_entry.focus()

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
            self.exercise_listbox.delete(self.selected_index)
            self.exercise_listbox.insert(self.selected_index, self.format_exercise(exercise))
        else:
            # Přidání nového
            self.exercises.append(exercise)
            self.exercise_listbox.insert(tk.END, self.format_exercise(exercise))

        self.update_count()
        self.clear_form()

    def format_exercise(self, exercise: Exercise) -> str:
        """Formátování cviku pro zobrazení v seznamu."""
        types = []
        if exercise.has_reps:
            types.append("opak.")
        if exercise.has_weight:
            types.append("váha")
        if exercise.has_time:
            types.append("čas")

        type_str = ", ".join(types) if types else "bez záznamu"
        return f"{exercise.name} [{type_str}]"

    def on_select(self, event):
        """Při výběru cviku v seznamu."""
        selection = self.exercise_listbox.curselection()
        if selection:
            self.selected_index = selection[0]

    def edit_exercise(self, event=None):
        """Úprava vybraného cviku."""
        if self.selected_index is None:
            selection = self.exercise_listbox.curselection()
            if not selection:
                return
            self.selected_index = selection[0]

        exercise = self.exercises[self.selected_index]

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

    def delete_exercise(self):
        """Smazání vybraného cviku."""
        selection = self.exercise_listbox.curselection()
        if not selection:
            return

        index = selection[0]
        exercise = self.exercises[index]

        if messagebox.askyesno("Potvrdit smazání", f"Opravdu smazat '{exercise.name}'?"):
            del self.exercises[index]
            self.exercise_listbox.delete(index)
            self.update_count()
            self.clear_form()

    def clear_all(self):
        """Smazání všech cviků."""
        if not self.exercises:
            return

        if messagebox.askyesno("Potvrdit smazání", "Opravdu smazat všechny cviky?"):
            self.exercises.clear()
            self.exercise_listbox.delete(0, tk.END)
            self.update_count()
            self.clear_form()

    def update_count(self):
        """Aktualizace počtu cviků."""
        count = len(self.exercises)
        if count == 0:
            text = "Celkem: 0 cviků"
        elif count == 1:
            text = "Celkem: 1 cvik"
        elif count < 5:
            text = f"Celkem: {count} cviky"
        else:
            text = f"Celkem: {count} cviků"
        self.count_label.config(text=text)

    def export_exercises(self):
        """Export cviků do JSON souboru."""
        if not self.exercises:
            messagebox.showwarning("Chyba", "Nejsou žádné cviky k exportu!")
            return

        filename = filedialog.asksaveasfilename(
            defaultextension=".json",
            filetypes=[("JSON soubory", "*.json"), ("Všechny soubory", "*.*")],
            initialfile="exercises_backup.json",
            title="Exportovat cviky"
        )

        if not filename:
            return

        # Přiřazení ID
        for i, exercise in enumerate(self.exercises, start=1):
            exercise.id = i

        backup_data = {
            "version": 1,
            "exportDate": int(datetime.now().timestamp() * 1000),
            "exercises": [ex.to_dict() for ex in self.exercises]
        }

        try:
            with open(filename, "w", encoding="utf-8") as f:
                json.dump(backup_data, f, ensure_ascii=False, indent=2)

            messagebox.showinfo("Hotovo", f"Exportováno {len(self.exercises)} cviků do:\n{filename}")
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
                    self.exercise_listbox.insert(tk.END, self.format_exercise(exercise))
                    existing_names.add(exercise.name.lower())
                    imported += 1

            self.update_count()

            skipped = len(data["exercises"]) - imported
            if skipped > 0:
                messagebox.showinfo("Hotovo", f"Importováno {imported} cviků.\nPřeskočeno {skipped} duplicit.")
            else:
                messagebox.showinfo("Hotovo", f"Importováno {imported} cviků.")

        except json.JSONDecodeError:
            messagebox.showerror("Chyba", "Neplatný JSON soubor!")
        except Exception as e:
            messagebox.showerror("Chyba", f"Nepodařilo se načíst soubor:\n{str(e)}")


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
