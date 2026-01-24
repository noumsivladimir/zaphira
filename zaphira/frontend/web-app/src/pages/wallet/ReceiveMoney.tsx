import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, QrCode, Share2, Download, Copy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { formatWalletNumber } from '@/lib/currency';

interface ReceiveMoneyProps {
  user: {
    name: string;
    walletNumber: string;
  };
  onBack: () => void;
}

export function ReceiveMoney({ user, onBack }: ReceiveMoneyProps) {
  const { t } = useTranslation();
  const { toast } = useToast();

  // Generate QR code URL using a free API
  const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${encodeURIComponent(`zaphira://pay/${user.walletNumber}`)}&bgcolor=ffffff&color=6366f1&margin=20`;

  const copyWalletNumber = () => {
    navigator.clipboard.writeText(user.walletNumber);
    toast({
      title: 'Copié',
      description: 'Numéro de portefeuille copié dans le presse-papier',
    });
  };

  const shareQRCode = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'Mon portefeuille Zaphira',
          text: `Envoyez-moi de l'argent sur Zaphira: ${user.walletNumber}`,
          url: `https://zaphira.app/pay/${user.walletNumber}`,
        });
      } catch (err) {
        console.error('Error sharing:', err);
      }
    } else {
      copyWalletNumber();
    }
  };

  const downloadQRCode = async () => {
    try {
      const response = await fetch(qrCodeUrl);
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `zaphira-${user.walletNumber}.png`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      toast({
        title: 'Téléchargé',
        description: 'QR Code enregistré dans vos téléchargements',
      });
    } catch (err) {
      toast({
        title: 'Erreur',
        description: 'Impossible de télécharger le QR Code',
        variant: 'destructive',
      });
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
        <div>
          <h1 className="text-lg font-semibold">{t('receive_money')}</h1>
          <p className="text-xs text-muted-foreground">Partagez votre QR code</p>
        </div>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-lg mx-auto px-4 py-8"
      >
        {/* User Info */}
        <Card className="p-6 text-center mb-6">
          <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-3">
            <QrCode className="w-8 h-8 text-primary" />
          </div>
          <h2 className="text-xl font-bold mb-1">{user.name}</h2>
          <p className="text-muted-foreground font-mono">
            {formatWalletNumber(user.walletNumber)}
          </p>
        </Card>

        {/* QR Code */}
        <Card className="p-6 mb-6">
          <div className="flex justify-center mb-4">
            <div className="bg-white p-4 rounded-lg">
              <img 
                src={qrCodeUrl}
                alt="QR Code"
                className="w-[280px] h-[280px]"
              />
            </div>
          </div>
          <p className="text-sm text-center text-muted-foreground">
            Scannez ce QR code pour m'envoyer de l'argent
          </p>
        </Card>

        {/* Wallet Number Card */}
        <Card className="p-4 mb-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs text-muted-foreground mb-1">Numéro de portefeuille</p>
              <p className="font-mono font-semibold">{user.walletNumber}</p>
            </div>
            <Button
              variant="ghost"
              size="icon"
              onClick={copyWalletNumber}
            >
              <Copy className="w-5 h-5" />
            </Button>
          </div>
        </Card>

        {/* Actions */}
        <div className="grid grid-cols-2 gap-3">
          <Button
            variant="outline"
            className="h-14"
            onClick={shareQRCode}
          >
            <Share2 className="w-5 h-5 mr-2" />
            Partager
          </Button>
          <Button
            variant="outline"
            className="h-14"
            onClick={downloadQRCode}
          >
            <Download className="w-5 h-5 mr-2" />
            Télécharger
          </Button>
        </div>

        {/* Instructions */}
        <Card className="p-4 mt-6 bg-primary/5 border-primary/20">
          <h3 className="font-semibold mb-2 text-sm">Comment recevoir de l'argent?</h3>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li className="flex gap-2">
              <span className="text-primary">1.</span>
              Partagez votre QR code ou numéro de portefeuille
            </li>
            <li className="flex gap-2">
              <span className="text-primary">2.</span>
              L'expéditeur scanne le code ou saisit votre numéro
            </li>
            <li className="flex gap-2">
              <span className="text-primary">3.</span>
              L'argent arrive instantanément dans votre portefeuille
            </li>
          </ul>
        </Card>
      </motion.div>
    </div>
  );
}

export default ReceiveMoney;
