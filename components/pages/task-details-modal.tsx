'use client'

import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"

interface Module {
  id: number;
  title: string;
  duration: string;
  completed: boolean;
  type: string;
  description: string;
}

interface TaskDetailsModalProps {
  module: Module | null;
  isOpen: boolean;
  onClose: () => void;
  onStart: () => void;
}

export default function TaskDetailsModal({ module, isOpen, onClose, onStart }: TaskDetailsModalProps) {
  if (!module) return null;

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{module.title}</DialogTitle>
          <DialogDescription>
            {module.description}
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="text-sm text-muted-foreground">
            <p><strong>Type:</strong> {module.type.charAt(0).toUpperCase() + module.type.slice(1)}</p>
            <p><strong>Duration:</strong> {module.duration}</p>
            <p><strong>Status:</strong> {module.completed ? "Completed" : "Not Started"}</p>
          </div>
        </div>
        <DialogFooter>
          <Button onClick={onClose} variant="outline">Close</Button>
          <Button onClick={onStart}>{module.completed ? "Review Again" : (module.type === 'quiz' ? "Start Quiz" : "Start Learning")}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
