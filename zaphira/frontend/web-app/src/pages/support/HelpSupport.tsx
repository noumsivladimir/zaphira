import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { 
  ChevronLeft, 
  MessageCircle, 
  Phone, 
  Mail, 
  HelpCircle,
  ChevronRight,
  FileText,
  Shield,
  CreditCard,
  Users
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Separator } from '@/components/ui/separator';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';

interface HelpSupportProps {
  onBack: () => void;
}

interface FAQItem {
  question: string;
  answer: string;
}

export function HelpSupport({ onBack }: HelpSupportProps) {
  const { t } = useTranslation();
  const { toast } = useToast();
  const [activeSection, setActiveSection] = useState<'faq' | 'contact'>('faq');
  const [contactForm, setContactForm] = useState({
    subject: '',
    message: '',
    email: '',
  });
  const [loading, setLoading] = useState(false);

  const faqs: FAQItem[] = [
    {
      question: 'Comment créer un compte Zaphira?',
      answer: 'Pour créer un compte, téléchargez l\'application, cliquez sur "Créer un compte", renseignez votre numéro de téléphone, vérifiez-le avec le code OTP, puis configurez votre code PIN et vos questions de sécurité.',
    },
    {
      question: 'Comment envoyer de l\'argent?',
      answer: 'Sur l\'écran d\'accueil, appuyez sur "Envoyer", entrez le numéro de portefeuille du destinataire, saisissez le montant, confirmez les détails et validez avec votre code PIN.',
    },
    {
      question: 'Quels sont les frais de transaction?',
      answer: 'Les frais varient selon le type de transaction:\n- Envoi d\'argent: 1% du montant (max 5,000 XAF)\n- Retrait: 500 XAF fixe\n- Rechargement: Gratuit\n- Transactions planifiées: Gratuit',
    },
    {
      question: 'Comment recharger mon portefeuille?',
      answer: 'Vous pouvez recharger votre portefeuille via Mobile Money (MTN, Orange), carte bancaire ou virement bancaire. Appuyez sur "Recharger" sur l\'écran d\'accueil et suivez les instructions.',
    },
    {
      question: 'Que faire si j\'oublie mon code PIN?',
      answer: 'Sur l\'écran de connexion, cliquez sur "Code PIN oublié?", vérifiez votre identité avec vos questions de sécurité, puis créez un nouveau code PIN.',
    },
    {
      question: 'Comment sécuriser mon compte?',
      answer: 'Activez l\'authentification à deux facteurs, utilisez un code PIN fort, ne partagez jamais vos identifiants, et configurez vos questions de sécurité.',
    },
    {
      question: 'Quelle est la limite d\'envoi d\'argent?',
      answer: 'Les limites par défaut sont:\n- Envoi: 500,000 XAF par transaction\n- Quotidien: 2,000,000 XAF\n- Mensuel: 10,000,000 XAF\nVous pouvez augmenter ces limites en vérifiant votre compte.',
    },
    {
      question: 'Combien de temps prend un transfert?',
      answer: 'Les transferts entre portefeuilles Zaphira sont instantanés. Les retraits vers Mobile Money ou compte bancaire prennent 24-48 heures.',
    },
  ];

  const contactMethods = [
    {
      icon: Phone,
      label: 'Téléphone',
      value: '+237 6XX XX XX XX',
      action: 'tel:+2376XXXXXXXX',
    },
    {
      icon: Mail,
      label: 'Email',
      value: 'support@zaphira.app',
      action: 'mailto:support@zaphira.app',
    },
    {
      icon: MessageCircle,
      label: 'WhatsApp',
      value: 'Chat en direct',
      action: 'https://wa.me/2376XXXXXXXX',
    },
  ];

  const helpTopics = [
    { icon: CreditCard, label: 'Transactions', count: 5 },
    { icon: Shield, label: 'Sécurité', count: 3 },
    { icon: Users, label: 'Compte', count: 4 },
    { icon: FileText, label: 'Documentation', count: 8 },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!contactForm.subject || !contactForm.message || !contactForm.email) {
      toast({
        title: 'Erreur',
        description: 'Veuillez remplir tous les champs',
        variant: 'destructive',
      });
      return;
    }

    setLoading(true);
    try {
      // API call to submit contact form
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      toast({
        title: 'Message envoyé',
        description: 'Notre équipe vous répondra dans les 24h',
      });

      setContactForm({ subject: '', message: '', email: '' });
    } catch (err) {
      toast({
        title: 'Erreur',
        description: 'Impossible d\'envoyer le message',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background pb-20">
      {/* Header */}
      <header className="safe-area-top px-4 py-4 border-b bg-background sticky top-0 z-10">
        <div className="flex items-center mb-4">
          <button
            onClick={onBack}
            className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center touch-target mr-3"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-lg font-semibold">{t('help_support')}</h1>
            <p className="text-xs text-muted-foreground">Nous sommes là pour vous aider</p>
          </div>
        </div>

        {/* Section Tabs */}
        <div className="flex gap-2">
          <button
            onClick={() => setActiveSection('faq')}
            className={cn(
              "flex-1 px-4 py-2 rounded-lg text-sm font-medium transition-colors",
              activeSection === 'faq'
                ? "bg-primary text-primary-foreground"
                : "bg-secondary text-muted-foreground"
            )}
          >
            <HelpCircle className="w-4 h-4 inline mr-2" />
            FAQ
          </button>
          <button
            onClick={() => setActiveSection('contact')}
            className={cn(
              "flex-1 px-4 py-2 rounded-lg text-sm font-medium transition-colors",
              activeSection === 'contact'
                ? "bg-primary text-primary-foreground"
                : "bg-secondary text-muted-foreground"
            )}
          >
            <MessageCircle className="w-4 h-4 inline mr-2" />
            Contact
          </button>
        </div>
      </header>

      <div className="max-w-lg mx-auto px-4 py-6">
        {activeSection === 'faq' ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Help Topics */}
            <div className="grid grid-cols-2 gap-3">
              {helpTopics.map((topic) => (
                <Card
                  key={topic.label}
                  className="p-4 cursor-pointer hover:bg-accent transition-colors"
                >
                  <topic.icon className="w-6 h-6 text-primary mb-2" />
                  <h3 className="font-medium text-sm mb-1">{topic.label}</h3>
                  <p className="text-xs text-muted-foreground">
                    {topic.count} articles
                  </p>
                </Card>
              ))}
            </div>

            {/* FAQ Accordion */}
            <div>
              <h2 className="text-lg font-semibold mb-4">Questions fréquentes</h2>
              <Accordion type="single" collapsible className="space-y-2">
                {faqs.map((faq, index) => (
                  <AccordionItem
                    key={index}
                    value={`item-${index}`}
                    className="bg-card rounded-lg px-4 border"
                  >
                    <AccordionTrigger className="hover:no-underline">
                      <span className="text-left font-medium">{faq.question}</span>
                    </AccordionTrigger>
                    <AccordionContent className="text-muted-foreground whitespace-pre-line">
                      {faq.answer}
                    </AccordionContent>
                  </AccordionItem>
                ))}
              </Accordion>
            </div>
          </motion.div>
        ) : (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Contact Methods */}
            <div>
              <h2 className="text-lg font-semibold mb-4">Nous contacter</h2>
              <div className="space-y-3">
                {contactMethods.map((method) => (
                  <a
                    key={method.label}
                    href={method.action}
                    className="block"
                  >
                    <Card className="p-4 hover:bg-accent transition-colors cursor-pointer">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
                          <method.icon className="w-6 h-6 text-primary" />
                        </div>
                        <div className="flex-1">
                          <h3 className="font-medium">{method.label}</h3>
                          <p className="text-sm text-muted-foreground">{method.value}</p>
                        </div>
                        <ChevronRight className="w-5 h-5 text-muted-foreground" />
                      </div>
                    </Card>
                  </a>
                ))}
              </div>
            </div>

            <Separator />

            {/* Contact Form */}
            <div>
              <h2 className="text-lg font-semibold mb-4">Envoyer un message</h2>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="text-sm font-medium mb-2 block">
                    Email
                  </label>
                  <Input
                    type="email"
                    placeholder="votre@email.com"
                    value={contactForm.email}
                    onChange={(e) => setContactForm({ ...contactForm, email: e.target.value })}
                    className="h-12"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium mb-2 block">
                    Sujet
                  </label>
                  <Input
                    type="text"
                    placeholder="En quoi pouvons-nous vous aider?"
                    value={contactForm.subject}
                    onChange={(e) => setContactForm({ ...contactForm, subject: e.target.value })}
                    className="h-12"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium mb-2 block">
                    Message
                  </label>
                  <Textarea
                    placeholder="Décrivez votre problème ou question..."
                    value={contactForm.message}
                    onChange={(e) => setContactForm({ ...contactForm, message: e.target.value })}
                    rows={6}
                    className="resize-none"
                  />
                </div>

                <Button
                  type="submit"
                  disabled={loading}
                  className="w-full h-12"
                >
                  {loading ? 'Envoi...' : 'Envoyer le message'}
                </Button>
              </form>
            </div>

            {/* Business Hours */}
            <Card className="p-4 bg-primary/5 border-primary/20">
              <h3 className="font-semibold mb-2 text-sm">Heures d'ouverture</h3>
              <div className="space-y-1 text-sm text-muted-foreground">
                <p>Lundi - Vendredi: 8h00 - 18h00</p>
                <p>Samedi: 9h00 - 15h00</p>
                <p>Dimanche: Fermé</p>
              </div>
            </Card>
          </motion.div>
        )}
      </div>
    </div>
  );
}

export default HelpSupport;
