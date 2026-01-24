import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, Shield, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { Skeleton } from '@/components/ui/skeleton';

interface SecurityQuestion {
  id: string;
  question: string;
  category: string;
}

interface SecurityQuestionsSetupProps {
  onComplete: () => void;
  onBack: () => void;
}

export function SecurityQuestionsSetup({ onComplete, onBack }: SecurityQuestionsSetupProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [questions, setQuestions] = useState<SecurityQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [selectedQuestions, setSelectedQuestions] = useState<string[]>(['', '', '']);
  const [answers, setAnswers] = useState<string[]>(['', '', '']);
  const [error, setError] = useState('');

  const MIN_QUESTIONS = 3;

  useEffect(() => {
    loadQuestions();
  }, []);

  const loadQuestions = async () => {
    setLoading(true);
    try {
      // Mock data - replace with API call
      // const data = await getSecurityQuestions();
      const mockQuestions: SecurityQuestion[] = [
        { id: '1', question: t('security_q_birth_city'), category: 'personal' },
        { id: '2', question: t('security_q_pet_name'), category: 'personal' },
        { id: '3', question: t('security_q_first_school'), category: 'education' },
        { id: '4', question: t('security_q_favorite_food'), category: 'preferences' },
        { id: '5', question: t('security_q_mothers_maiden'), category: 'family' },
        { id: '6', question: t('security_q_first_car'), category: 'personal' },
      ];
      setQuestions(mockQuestions);
    } catch (err) {
      toast({
        title: t('error'),
        description: t('error_loading_questions'),
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleQuestionChange = (index: number, questionId: string) => {
    const newSelected = [...selectedQuestions];
    newSelected[index] = questionId;
    setSelectedQuestions(newSelected);
    setError('');
  };

  const handleAnswerChange = (index: number, answer: string) => {
    const newAnswers = [...answers];
    newAnswers[index] = answer;
    setAnswers(newAnswers);
    setError('');
  };

  const getAvailableQuestions = (currentIndex: number) => {
    return questions.filter(
      (q) =>
        !selectedQuestions.includes(q.id) || selectedQuestions[currentIndex] === q.id
    );
  };

  const canSubmit = () => {
    return (
      selectedQuestions.every((q) => q) &&
      answers.every((a) => a.trim().length >= 2) &&
      new Set(selectedQuestions).size === MIN_QUESTIONS
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!canSubmit()) {
      setError(t('error_complete_all_questions'));
      return;
    }

    setSaving(true);
    setError('');

    try {
      // API call to save security questions
      // await setupSecurityQuestions(
      //   selectedQuestions.map((questionId, index) => ({
      //     questionId,
      //     answer: answers[index].trim(),
      //   }))
      // );

      toast({
        title: t('success'),
        description: t('security_questions_saved'),
      });

      onComplete();
    } catch (err: any) {
      setError(err.message || t('error_server'));
      toast({
        title: t('error'),
        description: err.message || t('error_server'),
        variant: 'destructive',
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 flex items-center border-b bg-background sticky top-0 z-10">
        <button
          onClick={onBack}
          className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-semibold">{t('security_questions')}</h1>
      </header>

      <div className="max-w-lg mx-auto px-4 py-8">
        {/* Info Banner */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 p-4 rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900 flex gap-3"
        >
          <Shield className="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
          <div className="text-sm text-blue-900 dark:text-blue-200">
            <p className="font-medium mb-1">{t('security_questions_info_title')}</p>
            <p>{t('security_questions_info_desc')}</p>
          </div>
        </motion.div>

        {loading ? (
          <div className="space-y-6">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="space-y-2">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ))}
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-6">
            {[...Array(MIN_QUESTIONS)].map((_, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className="space-y-3"
              >
                <Label>
                  {t('question')} {index + 1}
                </Label>
                <Select
                  value={selectedQuestions[index]}
                  onValueChange={(value) => handleQuestionChange(index, value)}
                  disabled={saving}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('select_question')} />
                  </SelectTrigger>
                  <SelectContent>
                    {getAvailableQuestions(index).map((question) => (
                      <SelectItem key={question.id} value={question.id}>
                        {question.question}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                {selectedQuestions[index] && (
                  <Input
                    type="text"
                    value={answers[index]}
                    onChange={(e) => handleAnswerChange(index, e.target.value)}
                    placeholder={t('enter_answer')}
                    disabled={saving}
                  />
                )}
              </motion.div>
            ))}

            {error && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 flex gap-2"
              >
                <AlertCircle className="w-5 h-5 text-destructive flex-shrink-0" />
                <p className="text-sm text-destructive">{error}</p>
              </motion.div>
            )}

            <Button
              type="submit"
              size="lg"
              className="w-full"
              disabled={!canSubmit() || saving}
            >
              {saving ? t('saving') : t('save_questions')}
            </Button>
          </form>
        )}
      </div>
    </div>
  );
}

export default SecurityQuestionsSetup;
